---
colors:
  primary: "#164da0"
  ink: "#172b46"
  muted: "#52647a"
  panel: "#eef3fa"
  surface: "#ffffff"
  border: "#b7c4d5"
  danger: "#a52332"
  focus: "#246bce"
typography:
  body:
    fontFamily: '"Segoe UI", Arial, sans-serif'
  display:
    fontFamily: 'Georgia, "Times New Roman", serif'
rounded:
  DEFAULT: "6px"
spacing:
  form-width: "390px"
components:
  input:
    minHeight: "50px"
  button:
    minHeight: "50px"
---

# IAM account access

The sign-in page borrows the feel of an institutional account desk: a blue identity panel, restrained serif statement, and direct form labels. It serves existing ISTAD account holders. English is the current UI language; no regional market policy is inferred.

Runtime tokens live in `src/main/resources/static/css/login.css`; this document mirrors its `:root` palette. Keep both synchronized. Body text uses system sans fonts and the identity statement uses Georgia. No external fonts or images are needed. Light theme with system forced-color support; no decorative motion.

The desktop split is 44% identity and the remaining space for a 390px form. Below 700px, the identity condenses into the brand header and the form flows naturally. Inputs and primary actions are at least 50px high, with 6px corners, visible keyboard focus, and reserved validation space. Scrollbars follow the palette and retain native behavior.

Use the same Sign in label throughout. Show errors inline in text, never only through color. Keep password values masked initially and out of client persistence. Busy feedback preserves button geometry. Do not add fake account creation, password recovery, social providers, or remember-me controls without backend support.

GitHub and Google login are backed by Spring Security OAuth2 Client. Provider links reuse the existing CSS tokens, native anchor navigation, and visible hover/focus/active states. Password login remains the primary action; provider failure returns to the same form with an inline error. Current runtime CSS owns the palette and typography; the older token values above predate the current page styling.
