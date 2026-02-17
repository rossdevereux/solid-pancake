import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ThemeProvider, CssBaseline } from '@mui/material';
import { theme } from './theme';
import { Layout } from './components/Layout';
import { ErrorBoundary } from './components/ErrorBoundary';
import { Dashboard } from './pages/Dashboard';
import { Templates } from './pages/Templates';
import { Batches } from './pages/Batches';
import { Login } from './pages/Login';

const queryClient = new QueryClient();

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <ErrorBoundary>
          <Router>
            <Routes>
              <Route path="/login" element={<Login />} />
              <Route path="/" element={<Layout><Dashboard /></Layout>} />
              <Route path="/templates" element={<Layout><Templates /></Layout>} />
              <Route path="/batches" element={<Layout><Batches /></Layout>} />
            </Routes>
          </Router>
        </ErrorBoundary>
      </ThemeProvider>
    </QueryClientProvider>
  );
}

export default App;
