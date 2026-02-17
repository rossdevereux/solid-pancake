import axios from 'axios';

export const apiClient = axios.create({
    baseURL: '/api/v1',
    headers: {
        'Content-Type': 'application/json',
    },
});

apiClient.interceptors.request.use((config) => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

export const fetchTemplates = async () => {
    const response = await apiClient.get('/templates');
    return response.data;
};

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const createTemplate = async (template: any) => {
    const response = await apiClient.post('/templates', template);
    return response.data;
};

export const fetchBatches = async () => {
    const response = await apiClient.get('/batches');
    return response.data;
};

export const createBatch = async (batch: { templateId: string; count: number }) => {
    const response = await apiClient.post('/batches', batch);
    return response.data;
};

export const fetchStats = async () => {
    const response = await apiClient.get('/stats');
    return response.data;
};

export const validateVoucher = async (code: string) => {
    const response = await apiClient.post('/vouchers/validate', { code });
    return response.data;
};

export const redeemVoucher = async (code: string, userId: string) => {
    const response = await apiClient.post('/vouchers/redeem', { code, userId });
    return response.data;
};
