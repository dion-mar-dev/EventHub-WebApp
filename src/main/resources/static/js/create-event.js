document.addEventListener('DOMContentLoaded', function () {
    
    // Character counters
    function updateCharCounter(inputId, counterId, maxLength) {
        const input = document.getElementById(inputId);
        const counter = document.getElementById(counterId);

        if (input && counter) {
            function updateCount() {
                const length = input.value.length;
                counter.textContent = length + ' / ' + maxLength;

                // Update counter color based on length
                counter.classList.remove('warning', 'danger');
                if (length > maxLength * 0.9) {
                    counter.classList.add('danger');
                } else if (length > maxLength * 0.75) {
                    counter.classList.add('warning');
                }
            }

            input.addEventListener('input', updateCount);
            updateCount(); // Initial count
        }
    }

    updateCharCounter('title', 'titleCounter', 100);
    updateCharCounter('description', 'descriptionCounter', 2000);
    updateCharCounter('location', 'locationCounter', 255);

    // Unlimited capacity checkbox handling
    const capacityInput = document.getElementById('capacity');
    const unlimitedCheckbox = document.getElementById('unlimitedCapacity');

    if (capacityInput && unlimitedCheckbox) {
        unlimitedCheckbox.addEventListener('change', function () {
            if (this.checked) {
                capacityInput.value = '';
                capacityInput.disabled = true;
                capacityInput.required = false;
            } else {
                capacityInput.disabled = false;
            }
        });

        // Check initial state
        if (unlimitedCheckbox.checked) {
            capacityInput.disabled = true;
        }
    }

    // Set minimum date to today
    const dateInput = document.getElementById('eventDate');
    if (dateInput) {
        const today = new Date().toISOString().split('T')[0];
        dateInput.setAttribute('min', today);
    }

    // Prevent double submission
    const form = document.querySelector('form');
    const submitBtn = document.getElementById('submitBtn');

    if (form && submitBtn) {
        form.addEventListener('submit', function (e) {
            // Check if date/time is in future
            const dateInput = document.getElementById('eventDate');
            const timeInput = document.getElementById('eventTime');

            if (dateInput && timeInput && dateInput.value && timeInput.value) {
                const eventDateTime = new Date(dateInput.value + 'T' + timeInput.value);
                const now = new Date();

                if (eventDateTime <= now) {
                    e.preventDefault();
                    alert('Event date and time must be in the future');
                    return false;
                }
            }

            // Disable submit button to prevent double submission
            submitBtn.disabled = true;
            submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i>Creating...';
        });
    }

    // Dynamic custom keyword management
    let customKeywordCounter = 0;
    const customKeywordsContainer = document.getElementById('customKeywordsContainer');
    const addCustomKeywordBtn = document.getElementById('addCustomKeywordBtn');
    const checkboxes = document.querySelectorAll('.keyword-checkbox');
    const keywordCount = document.getElementById('keywordCount');
    
    function createCustomKeywordInput() {
        const index = customKeywordCounter++;
        const div = document.createElement('div');
        div.className = 'input-group input-group-sm mb-2';
        div.innerHTML = `
            <input type="text" 
                   class="form-control custom-keyword-input" 
                   name="customKeywords"
                   maxlength="16"
                   placeholder="Type custom keyword">
            <button type="button" class="btn btn-outline-success btn-sm confirm-custom-keyword" title="Confirm keyword">
                <i class="fas fa-check"></i>
            </button>
            <button type="button" class="btn btn-outline-danger btn-sm remove-custom-keyword" title="Remove keyword">
                <i class="fas fa-times"></i>
            </button>
        `;
        
        // Add event listeners
        const input = div.querySelector('.custom-keyword-input');
        const confirmBtn = div.querySelector('.confirm-custom-keyword');
        const removeBtn = div.querySelector('.remove-custom-keyword');
        
        input.addEventListener('input', updateKeywordCount);
        
        // Enter key handler - trigger confirm button
        input.addEventListener('keydown', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                confirmBtn.click();
            }
        });
        
        // Confirm button - blur the input with haptic feedback
        confirmBtn.addEventListener('click', function() {
            input.blur();
            
            // Add confirmed styling to input
            input.classList.add('custom-keyword-confirmed');
            
            // Add haptic feedback animation
            confirmBtn.classList.add('confirm-feedback');
            
            // Remove animation class after animation completes
            setTimeout(() => {
                confirmBtn.classList.remove('confirm-feedback');
            }, 400);
        });
        
        removeBtn.addEventListener('click', function() {
            div.remove();
            updateKeywordCount();
        });
        
        return div;
    }
    
    function updateKeywordCount() {
        const checkedCount = document.querySelectorAll('.keyword-checkbox:checked').length;
        const customInputs = document.querySelectorAll('.custom-keyword-input');
        const customCount = Array.from(customInputs).filter(input => input.value.trim().length > 0).length;
        const total = checkedCount + customCount;
        
        if (keywordCount) {
            keywordCount.textContent = total;
        }
        
        // Disable/enable elements based on limit
        const atLimit = total >= 5;
        
        // Disable unchecked checkboxes if at limit
        checkboxes.forEach(cb => {
            if (!cb.checked) cb.disabled = atLimit;
        });
        
        // Disable add button if at limit
        if (addCustomKeywordBtn) {
            addCustomKeywordBtn.disabled = atLimit;
        }
        
        // Disable empty custom inputs if at limit
        customInputs.forEach(input => {
            if (input.value.trim().length === 0) {
                input.disabled = atLimit;
            }
        });
    }
    
    // Add custom keyword button handler
    if (addCustomKeywordBtn) {
        addCustomKeywordBtn.addEventListener('click', function(e) {
            e.preventDefault();
            
            try {
                if (customKeywordsContainer) {
                    const newInput = createCustomKeywordInput();
                    customKeywordsContainer.appendChild(newInput);
                    updateKeywordCount();
                    // Focus the new input
                    const inputField = newInput.querySelector('.custom-keyword-input');
                    if (inputField) {
                        inputField.focus();
                    }
                }
            } catch (error) {
                // Silently handle errors in production
            }
        });
    }
    
    // Existing keyword checkboxes
    checkboxes.forEach(cb => {
        cb.addEventListener('change', updateKeywordCount);
    });
    
    // Initial count
    updateKeywordCount();
});