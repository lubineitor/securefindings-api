$ErrorActionPreference = "Stop"

$ApiUrl = "http://localhost:8080/api/v1/findings"

Write-Host "Comprobando acceso sin token..."

$anonymousResponse = Invoke-WebRequest `
    -Uri $ApiUrl `
    -Method Get `
    -SkipHttpErrorCheck

if ($anonymousResponse.StatusCode -ne 401) {
    throw "Se esperaba HTTP 401 sin token; se recibió HTTP $($anonymousResponse.StatusCode)."
}

$anonymousBody = $anonymousResponse.Content | ConvertFrom-Json

if ($anonymousBody.code -ne "UNAUTHORIZED") {
    throw "La respuesta anónima no contiene el código UNAUTHORIZED."
}

Write-Host "OK: sin token, la API devuelve HTTP 401."

$accessToken = (Get-Clipboard -Raw).Trim()

if ([string]::IsNullOrWhiteSpace($accessToken)) {
    throw "El portapapeles no contiene un token. Ejecuta primero .\scripts\obtener-token.ps1."
}

try {
    Write-Host "Comprobando acceso con el token del portapapeles..."

    $authenticatedResponse = Invoke-WebRequest `
        -Uri $ApiUrl `
        -Method Get `
        -Headers @{ Authorization = "Bearer $accessToken" } `
        -SkipHttpErrorCheck

    if ($authenticatedResponse.StatusCode -ne 200) {
        throw "Se esperaba HTTP 200 con token; se recibió HTTP $($authenticatedResponse.StatusCode)."
    }

    $authenticatedBody = $authenticatedResponse.Content | ConvertFrom-Json

    if ($null -eq $authenticatedBody.PSObject.Properties["content"]) {
        throw "La respuesta autenticada no tiene el formato de una página de hallazgos."
    }

    Write-Host "OK: con token válido, la API devuelve HTTP 200."
}
finally {
    Remove-Variable accessToken -ErrorAction SilentlyContinue
}