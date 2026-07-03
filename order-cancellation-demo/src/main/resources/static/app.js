// Store orders in memory
let orders = [];
let currentFilter = 'ALL';

// DOM Elements
const createForm = document.getElementById('create-order-form');
const amountInput = document.getElementById('amount');
const delayInput = document.getElementById('delaySeconds');
const ordersGrid = document.getElementById('orders-grid');
const terminalBody = document.getElementById('terminal-body');
const clearLogsBtn = document.getElementById('clear-logs');
const filterBtns = document.querySelectorAll('.filter-btn');

// Start application
document.addEventListener('DOMContentLoaded', () => {
    initSSE();
    fetchOrders();
    startTimerThread();

    // Event listeners
    createForm.addEventListener('submit', handleCreateOrder);
    clearLogsBtn.addEventListener('click', clearLogs);
    
    filterBtns.forEach(btn => {
        btn.addEventListener('click', (e) => {
            filterBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            currentFilter = btn.dataset.filter;
            renderOrders();
        });
    });

    // Randomize initial amount for visual variety
    randomizeAmount();
});

// Fetch all existing orders
async function fetchOrders() {
    try {
        const response = await fetch('/api/orders');
        if (response.ok) {
            orders = await response.json();
            renderOrders();
        } else {
            addTerminalLine('system', 'Error fetching existing orders from backend.');
        }
    } catch (error) {
        addTerminalLine('system', 'Backend unreachable. Make sure Spring Boot is running.');
        console.error('Fetch error:', error);
    }
}

// Connect to Server-Sent Events
function initSSE() {
    addTerminalLine('system', 'Initializing Server-Sent Events stream...');
    const eventSource = new EventSource('/api/events');

    eventSource.addEventListener('connect', (event) => {
        addTerminalLine('connect', `[CONNECTED] ${event.data}`);
        document.querySelector('.status-indicator').className = 'status-indicator online';
        document.querySelector('.status-text').textContent = 'SSE Live Feed Connected';
    });

    eventSource.addEventListener('order-event', (event) => {
        try {
            const data = JSON.parse(event.data);
            const { order, eventType, message } = data;
            
            // Format log lines based on event type
            let logClass = 'system';
            if (eventType === 'CREATED') logClass = 'created';
            else if (eventType === 'PAID') logClass = 'paid';
            else if (eventType === 'CANCELLED') logClass = 'cancelled';
            else if (eventType === 'EXPIRED_CHECK') logClass = 'check';

            addTerminalLine(logClass, `[${eventType}] ${message}`);

            // Update local memory
            const index = orders.findIndex(o => o.id === order.id);
            if (index !== -1) {
                orders[index] = order;
            } else {
                orders.unshift(order); // Put new orders at the top
            }
            renderOrders();
        } catch (e) {
            console.error('Error parsing SSE event data:', e);
        }
    });

    eventSource.onerror = (error) => {
        console.error('SSE Error:', error);
        addTerminalLine('system', '[DISCONNECTED] Reconnecting to server event stream...');
        document.querySelector('.status-indicator').className = 'status-indicator';
        document.querySelector('.status-text').textContent = 'Disconnected';
    };
}

// Create mock order
async function handleCreateOrder(e) {
    e.preventDefault();
    const approach = document.querySelector('input[name="approach"]:checked').value;
    const amount = parseFloat(amountInput.value);
    const delaySeconds = parseInt(delayInput.value);

    const payload = { amount, approach, delaySeconds };

    try {
        const response = await fetch('/api/orders', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            // Trigger animation on button or inputs
            randomizeAmount();
        } else {
            addTerminalLine('system', 'Error creating order: ' + response.statusText);
        }
    } catch (error) {
        addTerminalLine('system', 'Failed to submit order request. Is server online?');
    }
}

// Pay order
async function payOrder(orderId) {
    try {
        const response = await fetch(`/api/orders/${orderId}/pay`, {
            method: 'PUT'
        });
        if (!response.ok) {
            addTerminalLine('system', `Error paying order #${orderId}`);
        }
    } catch (error) {
        console.error('Payment error:', error);
    }
}

// Render order cards
function renderOrders() {
    // Filter orders
    const filtered = orders.filter(order => {
        if (currentFilter === 'ALL') return true;
        return order.status === currentFilter;
    });

    if (filtered.length === 0) {
        ordersGrid.innerHTML = `
            <div class="empty-state">
                <i class="fa-solid fa-basket-shopping"></i>
                <p>No orders found matching filter "${currentFilter.toLowerCase()}".</p>
            </div>
        `;
        return;
    }

    ordersGrid.innerHTML = '';
    filtered.forEach(order => {
        const card = document.createElement('div');
        card.className = `order-card ${order.status.toLowerCase()}`;
        card.id = `order-card-${order.id}`;

        const isUnpaid = order.status === 'UNPAID';
        const formattedAmount = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(order.amount);
        
        // Expiration calculations
        const createdTime = new Date(order.createdAt).getTime();
        const expireTime = new Date(order.expireAt).getTime();
        const totalDurationMs = expireTime - createdTime;
        const now = Date.now();
        const remainingMs = Math.max(0, expireTime - now);
        const remainingSec = Math.ceil(remainingMs / 1000);
        
        let timerContent = '';
        let progressWidth = 100;
        
        if (isUnpaid) {
            progressWidth = (remainingMs / totalDurationMs) * 100;
            timerContent = `
                <div class="timer-label-row">
                    <span>Expiring in:</span>
                    <span class="timer-value" id="countdown-${order.id}">${remainingSec}s</span>
                </div>
                <div class="progress-bar-container">
                    <div class="progress-bar" id="progress-${order.id}" style="width: ${progressWidth}%"></div>
                </div>
            `;
        } else {
            timerContent = `
                <div class="timer-label-row">
                    <span>Status:</span>
                    <span class="status-pill ${order.status.toLowerCase()}">${order.status}</span>
                </div>
                <div class="progress-bar-container">
                    <div class="progress-bar" style="width: ${order.status === 'PAID' ? '100' : '0'}%"></div>
                </div>
            `;
        }

        const actionContent = isUnpaid ? `
            <div class="card-actions">
                <button class="btn-pay" onclick="payOrder(${order.id})">
                    <i class="fa-solid fa-credit-card"></i> Pay Now
                </button>
            </div>
        ` : '';

        card.innerHTML = `
            <div class="card-top">
                <span class="order-id">Order #${order.id}</span>
                <span class="approach-tag">${order.approach.replace('_', ' ')}</span>
            </div>
            <div class="card-amount">${formattedAmount}</div>
            <div class="card-timer-area">
                ${timerContent}
            </div>
            ${actionContent}
        `;

        ordersGrid.appendChild(card);
    });
}

// Background thread in browser to tick the countdown timers every 1s
function startTimerThread() {
    setInterval(() => {
        orders.forEach(order => {
            if (order.status !== 'UNPAID') return;

            const expireTime = new Date(order.expireAt).getTime();
            const createdTime = new Date(order.createdAt).getTime();
            const totalDurationMs = expireTime - createdTime;
            const now = Date.now();
            
            const remainingMs = Math.max(0, expireTime - now);
            const remainingSec = Math.ceil(remainingMs / 1000);
            
            const countdownEl = document.getElementById(`countdown-${order.id}`);
            const progressEl = document.getElementById(`progress-${order.id}`);
            
            if (countdownEl) {
                countdownEl.textContent = remainingSec > 0 ? `${remainingSec}s` : 'Processing...';
            }
            if (progressEl) {
                const progressWidth = (remainingMs / totalDurationMs) * 100;
                progressEl.style.width = `${progressWidth}%`;
            }
        });
    }, 1000);
}

// Utility: Add log line to terminal
function addTerminalLine(type, text) {
    const line = document.createElement('div');
    line.className = `terminal-line ${type}`;
    
    const timestamp = new Date().toLocaleTimeString();
    line.textContent = `[${timestamp}] ${text}`;
    
    terminalBody.appendChild(line);
    
    // Auto-scroll to bottom
    terminalBody.scrollTop = terminalBody.scrollHeight;

    // Cap terminal lines to 200
    while (terminalBody.childElementCount > 200) {
        terminalBody.removeChild(terminalBody.firstChild);
    }
}

// Clear terminal logs
function clearLogs() {
    terminalBody.innerHTML = '<div class="terminal-line system">[SYSTEM] Stream logs cleared. Waiting for new events...</div>';
}

// Utility: Randomize order amount
function randomizeAmount() {
    const randomVal = (Math.random() * 450 + 20).toFixed(2);
    amountInput.value = randomVal;
}
