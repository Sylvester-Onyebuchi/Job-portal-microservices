<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Verify Your Email</title>
</head>
<body style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f6f9; margin: 0; padding: 40px 0;">
<table align="center" border="0" cellpadding="0" cellspacing="0" width="100%" style="max-width: 600px; background-color: #ffffff; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); overflow: hidden;">
    <!-- Header -->
    <tr>
        <td style="background-color: #1a73e8; padding: 30px; text-align: center;">
            <h1 style="color: #ffffff; margin: 0; font-size: 24px; font-weight: 600;">Welcome!</h1>
        </td>
    </tr>
    <!-- Body -->
    <tr>
        <td style="padding: 40px 30px; color: #333333; line-height: 1.6;">
            <p style="font-size: 16px; margin-top: 0;">Hello,</p>
            <p style="font-size: 16px;">Thank you for registering. To complete your account setup and verify your email address, please click the button below:</p>

            <!-- Action Button -->
            <table border="0" cellpadding="0" cellspacing="0" style="margin: 30px auto;">
                <tr>
                    <td align="center" bgcolor="#1a73e8" style="border-radius: 4px;">
                        <a href="${link}" target="_blank" style="display: inline-block; padding: 14px 30px; font-size: 16px; color: #ffffff; text-decoration: none; font-weight: 600;">Verify Email Address</a>
                    </td>
                </tr>
            </table>

            <p style="font-size: 14px; color: #666666; font-style: italic;">This verification link will expire in ${linkExpirationInMinutes} minutes.</p>
            <hr style="border: 0; border-top: 1px solid #ebeeef; margin: 30px 0;">
            <p style="font-size: 12px; color: #999999; margin-bottom: 0;">If the button above doesn't work, copy and paste this URL into your browser:<br>
                <a href="${link}" style="color: #1a73e8; word-break: break-all;">${link}</a></p>
        </td>
    </tr>
</table>
</body>
</html>
