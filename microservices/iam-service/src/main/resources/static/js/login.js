/* ═══════════════════════════════════════════════════════════
   ISTAD Login — Enhanced JavaScript
   ═══════════════════════════════════════════════════════════ */

const form = document.querySelector('#login-form');
const password = document.querySelector('#password');
const toggle = document.querySelector('.password-toggle');
const submit = form.querySelector('[type="submit"]');
const submitText = submit.querySelector('.submit-text');
const submitArrow = submit.querySelector('.submit-arrow');
const submitSpinner = submit.querySelector('.submit-spinner');
let submitting = false;

/* ── Password visibility toggle ── */
toggle.hidden = false;
toggle.disabled = false;

toggle.addEventListener('click', () => {
    const visible = password.type === 'password';
    password.type = visible ? 'text' : 'password';

    // Toggle eye icons
    const eyeOpen = toggle.querySelector('.eye-open');
    const eyeClosed = toggle.querySelector('.eye-closed');
    eyeOpen.style.display = visible ? 'none' : 'block';
    eyeClosed.style.display = visible ? 'block' : 'none';

    toggle.setAttribute('aria-label', visible ? 'Hide password' : 'Show password');
    toggle.setAttribute('aria-pressed', String(visible));
    password.focus();
});

/* ── Form submission with validation ── */
form.addEventListener('submit', (event) => {
    if (event.isComposing || submitting) {
        event.preventDefault();
        return;
    }

    let firstInvalid;
    for (const name of ['username', 'password']) {
        const input = form.elements.namedItem(name);
        const invalid = input.value.length === 0;
        input.setAttribute('aria-invalid', String(invalid));
        document.querySelector(`#${name}-error`).textContent = invalid ? `Please enter your ${name}.` : '';

        // Add shake animation on invalid
        if (invalid) {
            const wrap = input.closest('.input-wrap');
            wrap.style.animation = 'none';
            wrap.offsetHeight; // Trigger reflow
            wrap.style.animation = 'shake 0.4s cubic-bezier(0.36, 0.07, 0.19, 0.97)';
        }

        if (invalid && !firstInvalid) firstInvalid = input;
    }

    if (firstInvalid) {
        event.preventDefault();
        firstInvalid.focus();
        return;
    }

    // Show loading state
    submitting = true;
    submit.disabled = true;
    form.setAttribute('aria-busy', 'true');

    if (submitArrow) submitArrow.style.display = 'none';
    if (submitSpinner) submitSpinner.style.display = 'block';
    if (submitText) submitText.textContent = 'Signing in…';

    document.querySelector('#submit-status').textContent = '';
});

/* ── Clear errors on input ── */
form.addEventListener('input', (event) => {
    const input = event.target;
    if (input.matches('input') && input.value.length > 0) {
        input.removeAttribute('aria-invalid');
        document.querySelector(`#${input.id}-error`).textContent = '';
    }
});

/* ── Reset state on page show (browser back/forward) ── */
window.addEventListener('pageshow', () => {
    submitting = false;
    submit.disabled = false;
    form.removeAttribute('aria-busy');

    if (submitArrow) submitArrow.style.display = 'block';
    if (submitSpinner) submitSpinner.style.display = 'none';
    if (submitText) submitText.textContent = 'Sign in';

    document.querySelector('#submit-status').textContent = '';
});

/* ── Add shake keyframe dynamically ── */
const style = document.createElement('style');
style.textContent = `
    @keyframes shake {
        10%, 90% { transform: translateX(-1px); }
        20%, 80% { transform: translateX(2px); }
        30%, 50%, 70% { transform: translateX(-3px); }
        40%, 60% { transform: translateX(3px); }
    }
`;
document.head.appendChild(style);

/* ── Input focus animations ── */
document.querySelectorAll('.input-wrap input').forEach(input => {
    input.addEventListener('focus', () => {
        input.closest('.input-wrap').classList.add('focused');
    });
    input.addEventListener('blur', () => {
        input.closest('.input-wrap').classList.remove('focused');
    });
});
