import { Typography, Grid, Paper, CircularProgress, Box, alpha } from '@mui/material';
import DashboardIcon from '@mui/icons-material/Dashboard';
import LayersIcon from '@mui/icons-material/Layers';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ConfirmationNumberIcon from '@mui/icons-material/ConfirmationNumber';
import { useQuery } from '@tanstack/react-query';
import { fetchStats } from '../api/client';
import { VoucherRedemption } from '../components/VoucherRedemption';

export const Dashboard: React.FC = () => {
    const { data: stats, isLoading } = useQuery({
        queryKey: ['stats'],
        queryFn: fetchStats,
        refetchInterval: 5000
    });

    if (isLoading) return <CircularProgress />;

    const cards = [
        { label: 'Total Vouchers', value: stats?.totalVouchers || 0, icon: <ConfirmationNumberIcon color="primary" />, color: 'primary' },
        { label: 'Processing Batches', value: stats?.pendingBatches || 0, icon: <LayersIcon color="secondary" />, color: 'secondary' },
        { label: 'Redeemed Today', value: stats?.redeemedToday || 0, icon: <CheckCircleIcon color="success" />, color: 'success' },
        { label: 'Active Vouchers', value: stats?.activeVouchers || 0, icon: <DashboardIcon color="info" />, color: 'info' },
    ];

    return (
        <div>
            <Typography variant="h4" gutterBottom sx={{ mb: 4 }}>
                Dashboard
            </Typography>
            <Grid container spacing={3}>
                {cards.map((card) => (
                    <Grid key={card.label} size={{ xs: 12, md: 3 }}>
                        <Paper sx={{ p: 3, display: 'flex', alignItems: 'center', gap: 2 }}>
                            <Box sx={{
                                p: 1.5,
                                bgcolor: (theme) => alpha(theme.palette[card.color as 'primary' | 'secondary' | 'success' | 'info'].main, 0.1),
                                borderRadius: 2,
                                display: 'flex'
                            }}>
                                {card.icon}
                            </Box>
                            <Box>
                                <Typography variant="body2" color="text.secondary">{card.label}</Typography>
                                <Typography variant="h4">{card.value}</Typography>
                            </Box>
                        </Paper>
                    </Grid>
                ))}
            </Grid>

            <VoucherRedemption />
        </div>
    );
};
