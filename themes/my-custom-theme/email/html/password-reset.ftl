<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Reset Your Password</title>
</head>
<body style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f6f9; margin: 0; padding: 40px 0;">
<table align="center" border="0" cellpadding="0" cellspacing="0" width="100%" style="max-width: 600px; background-color: #ffffff; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); overflow: hidden;">
    <!-- Header -->
    <tr>
        <td style="background-color: #2c3e50; padding: 30px; text-align: center;">
            <h1 style="color: #ffffff; margin: 0; font-size: 24px; font-weight: 600;">Password Reset Request</h1>
        </td>
    </tr>
    <!-- Body -->
    <tr>
        <td style="padding: 40px 30px; color: #333333; line-height: 1.6;">
            <p style="font-size: 16px; margin-top: 0;">Hello,</p>
            <p style="font-size: 16px;">We received a request to reset the password for your account. Click the button below to choose a new password:</p>

            <!-- Action Button -->
            <table border="0" cellpadding="0" cellspacing="0" style="margin: 30px auto;">
                <tr>
                    <td align="center" bgcolor="#e74c3c" style="border-radius: 4px;">
                        <a href="${link}" target="_blank" style="display: inline-block; padding: 14px 30px; font-size: 16px; color: #ffffff; text-decoration: none; font-weight: 600;">Reset Password</a>
                    </td>
                </tr>
            </table>

            <p style="font-size: 14px; color: #666666;">This link will remain valid for ${linkExpirationInMinutes} minutes. If you did not make this request, you can safely ignore this email; your password will remain unchanged.</p>
            <hr style="border: 0; border-top: 1px solid #ebeeef; margin: 30px 0;">
            <p style="font-size: 12px; color: #999999; margin-bottom: 0;">If the button above doesn't work, copy and paste this URL into your browser:<br>
                <a href="${link}" style="color: #e74c3c; word-break: break-all;">${link}</a></p>
        </td>
    </tr>
</table>
</body>
</html>
