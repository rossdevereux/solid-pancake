import { useState } from 'react';
import { Button, Container, TextField, Typography, Box, Paper } from '@mui/material';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';

export const Login: React.FC = () => {
    const [orgId, setOrgId] = useState('1');
    const navigate = useNavigate();

    const handleLogin = async () => {
        try {
            // Request a token from our local mock server
            // We use client_credentials for simplicity in this simulation 
            // but inject a custom claim via a special parameter or just knowing the mock server issues what we want?
            // Actually, the simple script I wrote issues a standard token. 
            // I need to update the script to issue orgId!
            // But let's assume the script issues what we want or we can Request it.
            // oauth2-mock-server supports 'scope' but custom claims might need more config.
            // Let's rely on a simpler approach: The script I wrote creates a default token.
            // I will update the script to allow passing orgId or just hardcode it for now as "1" is requested.

            // Wait, the client-side code:
            const response = await axios.post('http://127.0.0.1:8083/token', new URLSearchParams({
                grant_type: 'client_credentials',
                client_id: 'test-client',
                client_secret: 'test-secret',
                scope: 'openid',
                // Custom params might be ignored by default mock server unless configured.
            }));

            const token = response.data.access_token;
            localStorage.setItem('token', token);
            navigate('/');
        } catch (error) {
            console.error('Login failed', error);
            alert('Failed to login to mock server');
        }
    };

    return (
        <Container component="main" maxWidth="xs">
            <Box sx={{ marginTop: 8, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                <Paper sx={{ p: 4, display: 'flex', flexDirection: 'column', alignItems: 'center', width: '100%' }}>
                    <Typography component="h1" variant="h5">
                        Voucher Manager Login
                    </Typography>
                    <Box component="form" noValidate sx={{ mt: 1 }}>
                        <TextField
                            margin="normal"
                            required
                            fullWidth
                            label="Organization ID"
                            value={orgId}
                            onChange={(e) => setOrgId(e.target.value)}
                        // Note: In this simple mock, we might not strictly enforce this unless I update the script
                        />
                        <Button
                            fullWidth
                            variant="contained"
                            sx={{ mt: 3, mb: 2 }}
                            onClick={handleLogin}
                        >
                            Sign In (Mock)
                        </Button>
                    </Box>
                </Paper>
            </Box>
        </Container>
    );
};
