# Creates a Twilio whatsapp/authentication Content template and submits it for Meta approval.
# Requires: TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN
#
# Usage:
#   $env:TWILIO_ACCOUNT_SID = "AC..."
#   $env:TWILIO_AUTH_TOKEN = "..."
#   .\create-twilio-whatsapp-otp-template.ps1
#
# Optional:
#   $env:TWILIO_OTP_TEMPLATE_LANGUAGE = "fr"   # default: fr
#   $env:TWILIO_OTP_TEMPLATE_NAME = "nhub_otp" # default: nhub_otp
#   $env:TWILIO_OTP_CODE_EXPIRATION_MINUTES = "10"

param(
    [string]$Language = $(if ($env:TWILIO_OTP_TEMPLATE_LANGUAGE) { $env:TWILIO_OTP_TEMPLATE_LANGUAGE } else { "fr" }),
    [string]$FriendlyName = $(if ($env:TWILIO_OTP_TEMPLATE_NAME) { $env:TWILIO_OTP_TEMPLATE_NAME } else { "nhub_otp" }),
    [int]$CodeExpirationMinutes = $(if ($env:TWILIO_OTP_CODE_EXPIRATION_MINUTES) { [int]$env:TWILIO_OTP_CODE_EXPIRATION_MINUTES } else { 10 })
)

$ErrorActionPreference = "Stop"

$accountSid = $env:TWILIO_ACCOUNT_SID
$authToken = $env:TWILIO_AUTH_TOKEN

if ([string]::IsNullOrWhiteSpace($accountSid) -or [string]::IsNullOrWhiteSpace($authToken)) {
    Write-Error "Set TWILIO_ACCOUNT_SID and TWILIO_AUTH_TOKEN before running this script."
}

$pair = "${accountSid}:${authToken}"
$bytes = [System.Text.Encoding]::ASCII.GetBytes($pair)
$basic = [Convert]::ToBase64String($bytes)

$headers = @{
    Authorization = "Basic $basic"
    "Content-Type" = "application/json"
}

$createBody = @{
    friendly_name = $FriendlyName
    language = $Language
    types = @{
        "whatsapp/authentication" = @{
            add_security_recommendation = $true
            code_expiration_minutes = $CodeExpirationMinutes
            actions = @(
                @{
                    type = "COPY_CODE"
                    copy_code_text = "Copier le code"
                }
            )
        }
    }
} | ConvertTo-Json -Depth 6

Write-Host "Creating whatsapp/authentication template '$FriendlyName' (language=$Language)..."

$createResponse = Invoke-RestMethod `
    -Method Post `
    -Uri "https://content.twilio.com/v1/Content" `
    -Headers $headers `
    -Body $createBody

$contentSid = $createResponse.sid
Write-Host "ContentSid: $contentSid"

Write-Host "Submitting for WhatsApp Meta approval (category=AUTHENTICATION)..."

$approvalBody = @{
    name = $FriendlyName
    category = "AUTHENTICATION"
} | ConvertTo-Json

try {
    $approvalResponse = Invoke-RestMethod `
        -Method Post `
        -Uri "https://content.twilio.com/v1/Content/$contentSid/ApprovalRequests/whatsapp" `
        -Headers $headers `
        -Body $approvalBody
    Write-Host "Approval status: $($approvalResponse.status)"
} catch {
    Write-Warning "Approval submission failed (template may still be usable in sandbox after manual approval): $_"
}

Write-Host ""
Write-Host "Add to your .env:"
Write-Host "  TWILIO_WHATSAPP_OTP_CONTENT_SID=$contentSid"
Write-Host ""
Write-Host "Sandbox from (until production sender is registered):"
Write-Host "  TWILIO_WHATSAPP_FROM=+14155238886"
