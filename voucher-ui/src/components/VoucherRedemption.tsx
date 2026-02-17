import React, { useState } from 'react';
import { Paper, Typography, TextField, Button, Box, Alert, CircularProgress, Divider } from '@mui/material';
import { useMutation } from '@tanstack/react-query';
import { validateVoucher, redeemVoucher } from '../api/client';

export const VoucherRedemption: React.FC = () => {
    const [code, setCode] = useState('');
    const [userId, setUserId] = useState('demo-user');
    const [voucher, setVoucher] = useState<any>(null);

    const validateMutation = useMutation({
        mutationFn: validateVoucher,
        onSuccess: (data) => {
            setVoucher(data.voucher);
        }
    });

    const redeemMutation = useMutation({
        mutationFn: ({ code, userId }: { code: string; userId: string }) => redeemVoucher(code, userId),
        onSuccess: (data) => {
            setVoucher(data);
            alert('Voucher redeemed successfully!');
        }
    });

    const handleValidate = () => {
        setVoucher(null);
        validateMutation.mutate(code);
    };

    const handleRedeem = () => {
        redeemMutation.mutate({ code, userId });
    };

    return (
        <Paper sx={{ p: 3, mt: 3 }}>
            <Typography variant="h6" gutterBottom>Voucher Test Tool</Typography>
            <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
                <TextField
                    label="Voucher Code"
                    value={code}
                    onChange={(e) => setCode(e.target.value)}
                    size="small"
                />
                <TextField
                    label="User ID"
                    value={userId}
                    onChange={(e) => setUserId(e.target.value)}
                    size="small"
                />
                <Button variant="contained" onClick={handleValidate} disabled={!code || validateMutation.isPending}>
                    Validate
                </Button>
            </Box>

            {(validateMutation.isPending || redeemMutation.isPending) && <CircularProgress size={24} />}

            {validateMutation.error && (
                <Alert severity="error" sx={{ mt: 2 }}>
                    {(validateMutation.error as any).response?.data?.message || 'Error validating voucher'}
                </Alert>
            )}

            {redeemMutation.error && (
                <Alert severity="error" sx={{ mt: 2 }}>
                    {(redeemMutation.error as any).response?.data?.message || 'Error redeeming voucher'}
                </Alert>
            )}

            {voucher && (
                <Box sx={{ mt: 2 }}>
                    <Divider sx={{ my: 2 }} />
                    <Typography variant="subtitle1" color="primary">Voucher Details Found!</Typography>
                    <Typography variant="body2">Status: <strong>{voucher.status}</strong></Typography>
                    <Typography variant="body2">Usage: <strong>{voucher.usageCount} / {voucher.maxUsage}</strong></Typography>
                    {voucher.expiryDate && (
                        <Typography variant="body2">Expires: <strong>{new Date(voucher.expiryDate).toLocaleString()}</strong></Typography>
                    )}

                    {voucher.status === 'ACTIVE' && (
                        <Button
                            variant="outlined"
                            color="success"
                            onClick={handleRedeem}
                            sx={{ mt: 2 }}
                            disabled={redeemMutation.isPending}
                        >
                            Redeem Now
                        </Button>
                    )}
                </Box>
            )}
        </Paper>
    );
};
