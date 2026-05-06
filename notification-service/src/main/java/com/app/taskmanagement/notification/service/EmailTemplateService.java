package com.app.taskmanagement.notification.service;

import org.springframework.stereotype.Service;

@Service
public class EmailTemplateService {

	/* ───────────── Layout Wrapper (Bold Typography Theme) ───────────── */

	private String wrap(String content) {
		return """
				<!DOCTYPE html>
				<html>
				<head>
				  <meta charset="UTF-8">
				  <meta name="viewport" content="width=device-width, initial-scale=1.0">
				  <style>
				    @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800;900&family=Playfair+Display:ital,wght@1,700&family=JetBrains+Mono:wght@700&display=swap');
				    body { margin: 0; padding: 0; width: 100%% !important; -webkit-text-size-adjust: 100%%; -ms-text-size-adjust: 100%%; font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #F3F4F6; }
				  </style>
				</head>

				<body style="margin:0;background-color:#F3F4F6;padding:48px 16px;">

				  <table width="100%%" border="0" cellspacing="0" cellpadding="0">
				    <tr>
				      <td align="center">

				        <!-- CARD: Clean & Bold Elevation -->
				        <table width="580" style="background-color:#ffffff;border:1px solid rgba(226, 232, 240, 0.6);border-radius:16px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,0.02), 0 20px 40px -12px rgba(0,0,0,0.08);">

				          <!-- HEADER: Serif Logo + Tagline -->
				          <tr>
				            <td style="padding:32px 40px;border-bottom:1px solid #f1f5f9;">
				              <table width="100%%" border="0" cellspacing="0" cellpadding="0">
				                <tr>
				                  <td>
				                    <span style="font-size:10px;font-weight:900;color:#4f46e5;text-transform:uppercase;letter-spacing:0.3em;display:block;margin-bottom:4px;">FlowBoard Service</span>
				                    <div style="font-family:'Playfair Display',serif;font-style:italic;font-weight:700;font-size:24px;color:#0f172a;letter-spacing:-0.02em;">FlowBoard</div>
				                  </td>
				                  <td align="right">
				                    <div style="width:40px;height:40px;background-color:#eef2ff;border-radius:12px;text-align:center;">
				                      <span style="line-height:40px;font-size:20px;">⚡</span>
				                    </div>
				                  </td>
				                </tr>
				              </table>
				            </td>
				          </tr>

				          <!-- BODY -->
				          <tr>
				            <td style="padding:40px;">
				              %s
				            </td>
				          </tr>

				          <!-- FOOTER -->
				          <tr>
				            <td style="padding:24px 40px;background-color:rgba(249,250,251,0.5);border-top:1px solid #f1f5f9;">
				              <table width="100%%" border="0" cellspacing="0" cellpadding="0">
				                <tr>
				                  <td>
				                    <p style="margin:0;font-size:10px;font-weight:800;color:#94a3b8;text-transform:uppercase;letter-spacing:0.15em;">
				                      © 2026 FlowBoard HQ
				                    </p>
				                  </td>
				                  <td align="right">
				                    <div style="font-size:10px;font-weight:800;color:#cbd5e1;text-transform:uppercase;letter-spacing:0.1em;">
				                      HELP &bull; PRIVACY
				                    </div>
				                  </td>
				                </tr>
				              </table>
				            </td>
				          </tr>

				        </table>

				      </td>
				    </tr>
				  </table>

				</body>
				</html>
				"""
				.formatted(content);
	}

	/* ───────────── Entry Point ───────────── */

	public String buildHtmlEmail(String eventType, String name, String subject, String message, String triggeredBy) {
		String normalized = normalize(eventType);
		return switch (normalized) {
		case "OTP_EMAIL" -> buildOtp(message);
		case "CARD_ASSIGNED" -> infoCard("New card assigned", message, "TASK");
		case "COMMENT_ADDED" -> infoCard("New comment", message, "FEEDBACK");
		default -> generic(subject, message);
		};
	}

	/* ───────────── OTP EMAIL (⚡ Bold & Mono) ───────────── */

	private String buildOtp(String message) {
		String otp = extractOtp(message);
		return wrap(
				"""
						<h2 style="margin:0 0 16px;font-size:32px;font-weight:900;color:#0f172a;letter-spacing:-0.05em;line-height:1.1;">
						  Verification Required
						</h2>

						<p style="margin:0 0 40px;color:#64748b;font-size:16px;line-height:1.6;">
						  To maintain the security of your FlowBoard account, please enter the temporary access code provided below to complete your login.
						</p>

						<div style="background-color:#F8FAFC;border:1px solid #e2e8f0;border-radius:16px;padding:40px;text-align:center;margin-bottom:32px;">
						  <span style="text-transform:uppercase;font-size:11px;font-weight:800;letter-spacing:0.2em;color:#94a3b8;display:block;margin-bottom:24px;">Your Secure Code</span>
						  <div style="font-family:'JetBrains Mono',monospace;font-size:36px;font-weight:900;color:#4f46e5;letter-spacing:12px;margin-bottom:0;">
						    %s
						  </div>
						  <p style="margin:32px 0 0;font-size:12px;font-weight:600;color:#94a3b8;">
						    <span style="display:inline-block;width:8px;height:8px;background-color:#fb923c;border-radius:4px;margin-right:8px;"></span>
						    Expiring in 09:59 minutes
						  </p>
						</div>

						<table width="100%%" border="0" cellspacing="0" cellpadding="0" style="padding-top:32px;border-top:1px solid #f1f5f9;">
						  <tr>
						    <td width="32" style="vertical-align:top;color:#cbd5e1;font-size:20px;">🔒</td>
						    <td style="padding-left:16px;">
						      <p style="margin:0;font-size:11px;font-weight:600;color:#94a3b8;line-height:1.5;text-transform:uppercase;letter-spacing:0.05em;">
						        Never share this code. FlowBoard will never ask for your verification code via email.
						      </p>
						    </td>
						  </tr>
						</table>
						"""
						.formatted(otp));
	}

	/* ───────────── Info Card (No Buttons) ───────────── */

	private String infoCard(String title, String message, String context) {
		return wrap(
				"""
						<h2 style="margin:0 0 12px;font-size:28px;font-weight:900;color:#0f172a;letter-spacing:-0.04em;">
						  %s
						</h2>

						<p style="margin:0 0 32px;color:#64748b;font-size:16px;">
						  You have received a new update in your FlowBoard workspace.
						</p>

						<div style="border:2px solid #f1f5f9;border-radius:16px;padding:32px;background-color:#ffffff;margin-bottom:32px;box-shadow:0 4px 6px -1px rgba(0,0,0,0.05);">
						  <div style="font-size:10px;font-weight:900;color:#4f46e5;text-transform:uppercase;letter-spacing:0.2em;margin-bottom:8px;">%s</div>
						  <p style="margin:0;color:#0f172a;font-size:16px;line-height:1.7;font-weight:500;">
						    %s
						  </p>
						</div>

						<div style="background-color:#f1f5f9;border-radius:8px;padding:12px 16px;display:inline-block;">
						  <p style="margin:0;font-size:12px;font-weight:700;color:#94a3b8;text-transform:uppercase;letter-spacing:0.05em;">
						    Please check your dashboard for full details.
						  </p>
						</div>
						"""
						.formatted(title, context, escape(message)));
	}

	/* ───────────── Support Helpers ───────────── */

	private String extractOtp(String msg) {
		if (msg == null)
			return "------";
		String numeric = msg.replaceAll("[^0-9]", "");
		return numeric.length() >= 6 ? numeric.substring(0, 6) : numeric;
	}

	private String normalize(String type) {
		if (type == null)
			return "";
		return type.trim().replace(" ", "_").toUpperCase();
	}

	private String escape(String text) {
		if (text == null)
			return "";
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private String generic(String subject, String message) {
		return wrap("""
				<h2 style="margin:0 0 12px;font-size:28px;font-weight:900;color:#0f172a;letter-spacing:-0.04em;">%s</h2>
				<p style="color:#64748b;font-size:16px;line-height:1.6;">%s</p>
				""".formatted(escape(subject), escape(message)));
	}
}