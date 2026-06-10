(() => {
    const splitTags = (value) => value
            .split(/[;,]/)
            .map((tag) => tag.trim().replace(/\s+/g, ' '))
            .filter(Boolean);

    const tagKey = (value) => value.toLowerCase();

    const initTagEditor = (editor) => {
        const hiddenInput = editor.querySelector('[data-tag-value]');
        const selectedTags = editor.querySelector('[data-tag-selected]');
        const customInput = editor.querySelector('[data-tag-custom]');
        const addButton = editor.querySelector('[data-tag-add]');
        const optionButtons = Array.from(editor.querySelectorAll('[data-tag-option]'));
        let tags = splitTags(hiddenInput.value);

        const hasTag = (tag) => tags.some((value) => tagKey(value) === tagKey(tag));

        const addTag = (tag) => {
            if (!hasTag(tag)) {
                tags.push(tag);
            }
        };

        const removeTag = (tag) => {
            tags = tags.filter((value) => tagKey(value) !== tagKey(tag));
        };

        const syncOptions = () => {
            optionButtons.forEach((button) => {
                const value = button.dataset.tagOption;
                const selected = hasTag(value);
                button.classList.toggle('is-selected', selected);
                button.setAttribute('aria-pressed', selected);
            });
        };

        const renderSelectedTags = () => {
            selectedTags.innerHTML = '';

            tags.forEach((tag) => {
                const token = document.createElement('span');
                token.className = 'tag-token';

                const label = document.createElement('span');
                label.textContent = tag;

                const removeButton = document.createElement('button');
                removeButton.type = 'button';
                removeButton.className = 'tag-token__remove';
                removeButton.setAttribute('aria-label', `Remove ${tag}`);
                removeButton.textContent = 'x';
                removeButton.addEventListener('click', () => {
                    removeTag(tag);
                    update();
                });

                token.append(label, removeButton);
                selectedTags.appendChild(token);
            });
        };

        const update = () => {
            hiddenInput.value = tags.join(', ');
            renderSelectedTags();
            syncOptions();
        };

        optionButtons.forEach((button) => {
            button.type = 'button';
            button.setAttribute('aria-pressed', false);
            button.addEventListener('click', () => {
                const value = button.dataset.tagOption;

                if (hasTag(value)) {
                    removeTag(value);
                } else {
                    addTag(value);
                }

                update();
            });
        });

        const addCustomTags = () => {
            splitTags(customInput.value).forEach(addTag);
            customInput.value = '';
            update();
        };

        addButton.addEventListener('click', addCustomTags);
        customInput.addEventListener('keydown', (event) => {
            if (event.key === 'Enter') {
                event.preventDefault();
                addCustomTags();
            }
        });

        update();
    };

    const initAllTagEditors = () => {
        document.querySelectorAll('[data-tag-editor]').forEach(initTagEditor);
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initAllTagEditors);
    } else {
        initAllTagEditors();
    }
})();
