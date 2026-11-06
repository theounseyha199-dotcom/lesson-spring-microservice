document.querySelectorAll('[data-password]').forEach(button => {
    button.hidden = false;
    button.addEventListener('click', () => {
        const input = document.getElementById(button.dataset.password);
        const showing = input.type === 'password';
        input.type = showing ? 'text' : 'password';
        button.setAttribute('aria-pressed', String(showing));
        button.setAttribute('aria-label', showing ? button.dataset.hide : button.dataset.show);
        button.textContent = showing ? button.dataset.hideText : button.dataset.showText;
        input.focus();
    });
});
let dirty = false;
document.querySelectorAll('form[data-dirty]').forEach(form => {
    form.addEventListener('input', () => { dirty = true; });
});
document.querySelectorAll('form[data-submit]').forEach(form => {
    form.addEventListener('submit', event => {
        if (event.isComposing || form.getAttribute('aria-busy') === 'true') { event.preventDefault(); return; }
        dirty = false;
        form.setAttribute('aria-busy', 'true');
        form.querySelectorAll('button[type="submit"]').forEach(button => { button.disabled = true; });
    });
});
window.addEventListener('pageshow', () => {
    document.querySelectorAll('form[data-submit]').forEach(form => {
        form.removeAttribute('aria-busy');
        form.querySelectorAll('button[type="submit"]').forEach(button => { button.disabled = false; });
    });
});
window.addEventListener('beforeunload', event => {
    if (dirty) { event.preventDefault(); event.returnValue = ''; }
});
const invalid = document.querySelector('[aria-invalid="true"]') || document.querySelector('[role="alert"][tabindex]');
if (invalid) invalid.focus();
