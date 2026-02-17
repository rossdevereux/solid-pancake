import { Component, type ErrorInfo, type ReactNode } from 'react';
import { Alert, Box, Typography, Button } from '@mui/material';

interface Props {
    children: ReactNode;
}

interface State {
    hasError: boolean;
    error?: Error;
}

export class ErrorBoundary extends Component<Props, State> {
    public state: State = {
        hasError: false
    };

    public static getDerivedStateFromError(error: Error): State {
        return { hasError: true, error };
    }

    public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
        console.error('Uncaught error:', error, errorInfo);
    }

    public render() {
        if (this.state.hasError) {
            return (
                <Box sx={{ p: 4, textAlign: 'center' }}>
                    <Alert severity="error" sx={{ mb: 2 }}>
                        <Typography variant="h6">Something went wrong.</Typography>
                        {this.state.error?.message}
                    </Alert>
                    <Button variant="contained" onClick={() => window.location.reload()}>
                        Reload Page
                    </Button>
                </Box>
            );
        }

        return this.props.children;
    }
}
