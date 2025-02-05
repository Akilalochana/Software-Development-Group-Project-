const express = require('express');
const cors = require('cors');
const fs = require('fs').promises;
const path = require('path');

const app = express();
const PORT = 5000;

// Middleware
app.use(cors());
app.use(express.json());

// Path to store messages
const MESSAGES_FILE = path.join(__dirname, 'messages.json');

// Initialize messages file if it doesn't exist
async function initializeMessagesFile() {
    try {
        await fs.access(MESSAGES_FILE);
    } catch {
        await fs.writeFile(MESSAGES_FILE, JSON.stringify([], null, 2));
    }
}

// API endpoint to save messages
app.post('/api/messages', async (req, res) => {
    try {
        const message = {
            id: Date.now(),
            timestamp: new Date().toISOString(),
            ...req.body
        };

        // Read existing messages
        const messagesData = await fs.readFile(MESSAGES_FILE, 'utf8');
        const messages = JSON.parse(messagesData);

        // Add new message
        messages.push(message);

        // Save updated messages
        await fs.writeFile(MESSAGES_FILE, JSON.stringify(messages, null, 2));

        res.status(201).json({ success: true, message: 'Message saved successfully' });
    } catch (error) {
        console.error('Error saving message:', error);
        res.status(500).json({ success: false, error: 'Failed to save message' });
    }
});

// Initialize and start server
initializeMessagesFile().then(() => {
    app.listen(PORT, () => {
        console.log(`Server running on port ${PORT}`);
    });
});
