$ErrorActionPreference = "Stop"

# Ajustes de tu instancia local de Keycloak
$KeycloakBaseUrl = "http://localhost:8081"
$Realm = "securefindings"
$ClientId = "securefindings-cli"
$ExpectedAudience = "securefindings-api"
$Port = 8765

$RedirectUri = "http://127.0.0.1:$Port/callback/"

function ConvertTo-Base64Url {
    param(
        [Parameter(Mandatory = $true)]
        [byte[]]$Bytes
    )

    return [Convert]::ToBase64String($Bytes).
    TrimEnd("=").
    Replace("+", "-").
    Replace("/", "_")
}

function New-CryptoRandomString {
    param(
        [Parameter(Mandatory = $true)]
        [int]$ByteCount
    )

    $bytes = [byte[]]::new($ByteCount)
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()

    try {
        $generator.GetBytes($bytes)
    }
    finally {
        $generator.Dispose()
    }

    return ConvertTo-Base64Url -Bytes $bytes
}

function Send-CallbackResponse {
    param(
        [Parameter(Mandatory = $true)]
        [System.Net.Sockets.NetworkStream]$Stream,

        [Parameter(Mandatory = $true)]
        [int]$StatusCode,

        [Parameter(Mandatory = $true)]
        [string]$StatusText,

        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    $html = @"
<!doctype html>
<html lang="es">
<head><meta charset="utf-8"><title>SecureFindings</title></head>
<body><h1>$Message</h1><p>Ya puedes cerrar esta ventana.</p></body>
</html>
"@

    $bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($html)
    $headers = @(
        "HTTP/1.1 $StatusCode $StatusText"
        "Content-Type: text/html; charset=utf-8"
        "Content-Length: $($bodyBytes.Length)"
        "Connection: close"
        ""
        ""
    ) -join "`r`n"

    $headerBytes = [System.Text.Encoding]::ASCII.GetBytes($headers)
    $Stream.Write($headerBytes, 0, $headerBytes.Length)
    $Stream.Write($bodyBytes, 0, $bodyBytes.Length)
    $Stream.Flush()
}

$baseUrl = $KeycloakBaseUrl.TrimEnd("/")
$authorizationEndpoint = "$baseUrl/realms/$Realm/protocol/openid-connect/auth"
$tokenEndpoint = "$baseUrl/realms/$Realm/protocol/openid-connect/token"

$state = New-CryptoRandomString -ByteCount 32
$codeVerifier = New-CryptoRandomString -ByteCount 32
$verifierBytes = [System.Text.Encoding]::ASCII.GetBytes($codeVerifier)
$sha256 = [System.Security.Cryptography.SHA256]::Create()

try {
    $challengeBytes = $sha256.ComputeHash($verifierBytes)
}
finally {
    $sha256.Dispose()
}

$codeChallenge = ConvertTo-Base64Url -Bytes $challengeBytes

$authorizationParameters = @(
    "client_id=$([Uri]::EscapeDataString($ClientId))"
    "response_type=code"
    "scope=$([Uri]::EscapeDataString('openid profile'))"
    "redirect_uri=$([Uri]::EscapeDataString($RedirectUri))"
    "state=$([Uri]::EscapeDataString($state))"
    "code_challenge=$([Uri]::EscapeDataString($codeChallenge))"
    "code_challenge_method=S256"
)

$authorizationUrl = "{0}?{1}" -f `
    $authorizationEndpoint, `
    ($authorizationParameters -join "&")

if (-not $authorizationUrl.StartsWith("http://", [StringComparison]::OrdinalIgnoreCase) -and
    -not $authorizationUrl.StartsWith("https://", [StringComparison]::OrdinalIgnoreCase)) {
    throw "La URL de autorización no es absoluta: $authorizationUrl"
}

$listener = [System.Net.Sockets.TcpListener]::new(
    [System.Net.IPAddress]::Loopback,
    $Port
)

$client = $null
$reader = $null

try {
    try {
        $listener.Start()
    }
    catch {
        throw "No se pudo abrir el puerto local $Port. Comprueba si otra aplicación lo está usando. Detalle: $($_.Exception.Message)"
    }

    Write-Host "Abriendo Keycloak en el navegador..."
    Start-Process -FilePath $authorizationUrl

    $client = $listener.AcceptTcpClient()
    $stream = $client.GetStream()
    $reader = [System.IO.StreamReader]::new(
        $stream,
        [System.Text.Encoding]::ASCII,
        $false,
        1024,
        $true
    )

    $requestLine = $reader.ReadLine()

    if ([string]::IsNullOrWhiteSpace($requestLine)) {
        Send-CallbackResponse $stream 400 "Bad Request" "Solicitud no válida."
        throw "Keycloak no devolvió una solicitud de callback válida."
    }

    $requestParts = $requestLine.Split(" ")

    if ($requestParts.Length -lt 2 -or $requestParts[0] -ne "GET") {
        Send-CallbackResponse $stream 400 "Bad Request" "Solicitud no válida."
        throw "El callback recibido no es una petición GET válida."
    }

    do {
        $headerLine = $reader.ReadLine()
    } while ($null -ne $headerLine -and $headerLine.Length -gt 0)

    $callbackUri = [Uri]::new(
        "http://127.0.0.1:$Port$($requestParts[1])"
    )

    if ($callbackUri.AbsolutePath -ne "/callback/") {
        Send-CallbackResponse $stream 400 "Bad Request" "Ruta de callback no válida."
        throw "La ruta del callback recibido no coincide con la configurada."
    }

    $callbackParameters = @{}
    $rawQuery = $callbackUri.Query.TrimStart("?")

    foreach ($pair in $rawQuery.Split("&")) {
        if ([string]::IsNullOrEmpty($pair)) {
            continue
        }

        $separator = $pair.IndexOf("=")

        if ($separator -lt 0) {
            $name = [Uri]::UnescapeDataString($pair.Replace("+", " "))
            $value = ""
        }
        else {
            $name = [Uri]::UnescapeDataString(
                $pair.Substring(0, $separator).Replace("+", " ")
            )
            $value = [Uri]::UnescapeDataString(
                $pair.Substring($separator + 1).Replace("+", " ")
            )
        }

        $callbackParameters[$name] = $value
    }

    if (-not $callbackParameters.ContainsKey("state") -or
        -not [string]::Equals(
            $callbackParameters["state"],
            $state,
            [System.StringComparison]::Ordinal
        )) {

        Send-CallbackResponse $stream 400 "Bad Request" "Verificación de autenticación fallida."
        throw "El parámetro state del callback no es válido."
    }

    if ($callbackParameters.ContainsKey("error")) {
        Send-CallbackResponse $stream 400 "Bad Request" "Keycloak rechazó la autenticación."
        $description = $callbackParameters["error_description"]
        throw "Keycloak rechazó la autenticación: $($callbackParameters['error']) $description"
    }

    if (-not $callbackParameters.ContainsKey("code") -or
        [string]::IsNullOrWhiteSpace($callbackParameters["code"])) {

        Send-CallbackResponse $stream 400 "Bad Request" "No se recibió el código de autorización."
        throw "El callback no contiene un código de autorización."
    }

    $tokenForm = @{
        grant_type    = "authorization_code"
        client_id     = $ClientId
        code          = $callbackParameters["code"]
        redirect_uri  = $RedirectUri
        code_verifier = $codeVerifier
    }

    try {
        $tokenResponse = Invoke-RestMethod `
            -Method Post `
            -Uri $tokenEndpoint `
            -ContentType "application/x-www-form-urlencoded" `
            -Body $tokenForm
    }
    catch {
        Send-CallbackResponse $stream 400 "Bad Request" "No se pudo obtener el token."
        throw "Falló el intercambio del código por el token. Comprueba la configuración PKCE y la URI de redirección. Detalle: $($_.Exception.Message)"
    }

    if ([string]::IsNullOrWhiteSpace($tokenResponse.access_token)) {
        Send-CallbackResponse $stream 400 "Bad Request" "Keycloak no devolvió un access token."
        throw "La respuesta de Keycloak no contiene un access token."
    }

    # Lee el claim aud para comprobar que se solicitó el token de esta API.
    # Esta lectura local no valida la firma; la API realiza esa validación.
    $tokenParts = $tokenResponse.access_token.Split(".")

    if ($tokenParts.Length -ne 3) {
        Send-CallbackResponse $stream 400 "Bad Request" "El token recibido no tiene el formato esperado."
        throw "El access token no tiene formato JWT."
    }

    $payloadBase64 = $tokenParts[1].Replace("-", "+").Replace("_", "/")
    $payloadBase64 += "=" * ((4 - ($payloadBase64.Length % 4)) % 4)

    $payloadJson = [System.Text.Encoding]::UTF8.GetString(
        [Convert]::FromBase64String($payloadBase64)
    )

    $claims = $payloadJson | ConvertFrom-Json
    $audiences = @()

    if ($null -ne $claims.aud) {
        $audiences = @($claims.aud | ForEach-Object { [string]$_ })
    }

    if ($audiences -notcontains $ExpectedAudience) {
        Send-CallbackResponse $stream 400 "Bad Request" "El token no incluye la audiencia esperada."
        throw "El token no incluye la audiencia '$ExpectedAudience'. Revisa el mapper de audiencia en Keycloak."
    }

    Set-Clipboard -Value $tokenResponse.access_token
    Send-CallbackResponse $stream 200 "OK" "Autenticación completada."

    $expirySeconds = $tokenResponse.expires_in
    Write-Host "Token copiado al portapapeles."
    Write-Host "Audiencia comprobada: $ExpectedAudience"
    Write-Host "Duración informada por Keycloak: $expirySeconds segundos."
}
finally {
    if ($null -ne $reader) {
        $reader.Dispose()
    }

    if ($null -ne $client) {
        $client.Close()
    }

    $listener.Stop()
}