import React, { useState } from 'react';
import { Typography, Button, TextField, Dialog, DialogTitle, DialogContent, DialogActions, MenuItem, Table, TableBody, TableCell, TableHead, TableRow, Paper, Box, TableContainer, Chip } from '@mui/material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { fetchBatches, createBatch, fetchTemplates } from '../api/client';

export const Batches: React.FC = () => {
    const [open, setOpen] = useState(false);
    const [templateId, setTemplateId] = useState('');
    const [count, setCount] = useState(100);

    const queryClient = useQueryClient();

    const { data: batches } = useQuery({ queryKey: ['batches'], queryFn: fetchBatches });
    const { data: templates } = useQuery({ queryKey: ['templates'], queryFn: fetchTemplates });

    const mutation = useMutation({
        mutationFn: createBatch,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['batches'] });
            setOpen(false);
        },
    });

    const handleSubmit = () => {
        mutation.mutate({ templateId, count });
    };

    return (
        <div>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
                <Typography variant="h4">Voucher Batches</Typography>
                <Button variant="contained" onClick={() => setOpen(true)}>
                    Create Batch
                </Button>
            </Box>

            <Paper sx={{ width: '100%', overflow: 'hidden' }}>
                <TableContainer>
                    <Table>
                        <TableHead>
                            <TableRow>
                                <TableCell>ID</TableCell>
                                <TableCell>Template</TableCell>
                                <TableCell>Quantity</TableCell>
                                <TableCell>Status</TableCell>
                                <TableCell>Created</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
                            {batches?.map((batch: any) => (
                                <TableRow key={batch.id} hover>
                                    <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.8rem' }}>{batch.id}</TableCell>
                                    <TableCell>{batch.templateId}</TableCell>
                                    <TableCell sx={{ fontWeight: 600 }}>{batch.quantity}</TableCell>
                                    <TableCell>
                                        <Chip
                                            label={batch.status}
                                            color={batch.status === 'ACTIVE' ? 'success' : 'warning'}
                                            size="small"
                                            variant="outlined"
                                        />
                                    </TableCell>
                                    <TableCell color="text.secondary">
                                        {new Date(batch.createdDate).toLocaleDateString()}
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </TableContainer>
            </Paper>

            <Dialog open={open} onClose={() => setOpen(false)}>
                <DialogTitle>Generate Batch</DialogTitle>
                <DialogContent>
                    <TextField
                        select
                        margin="dense"
                        label="Template"
                        fullWidth
                        value={templateId}
                        onChange={(e) => setTemplateId(e.target.value)}
                    >
                        {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
                        {templates?.map((t: any) => (
                            <MenuItem key={t.id} value={t.id}>{t.name}</MenuItem>
                        ))}
                    </TextField>
                    <TextField margin="dense" label="Count" type="number" fullWidth value={count} onChange={(e) => setCount(Number(e.target.value))} />
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setOpen(false)}>Cancel</Button>
                    <Button onClick={handleSubmit} variant="contained">Generate</Button>
                </DialogActions>
            </Dialog>
        </div>
    );
};
