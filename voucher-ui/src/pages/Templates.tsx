import React, { useState } from 'react';
import { Typography, Button, TextField, Dialog, DialogTitle, DialogContent, DialogActions, Table, TableBody, TableCell, TableHead, TableRow, Paper, Box, TableContainer } from '@mui/material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { fetchTemplates, createTemplate } from '../api/client';

export const Templates: React.FC = () => {
    const [open, setOpen] = useState(false);
    const [name, setName] = useState('');
    const [codeFormat, setCodeFormat] = useState('GIFT-####-????');
    const [validityDays, setValidityDays] = useState(30);
    const [redemptionLimit, setRedemptionLimit] = useState(1);

    const queryClient = useQueryClient();

    const { data: templates } = useQuery({ queryKey: ['templates'], queryFn: fetchTemplates });

    const mutation = useMutation({
        mutationFn: createTemplate,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['templates'] });
            setOpen(false);
        },
    });

    const handleSubmit = () => {
        mutation.mutate({
            name,
            codeFormat,
            validityPeriod: {
                type: 'DURATION',
                durationDays: validityDays
            },
            redemptionLimit: {
                limitPerUser: redemptionLimit
            }
        });
    };

    return (
        <div>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
                <Typography variant="h4">Voucher Templates</Typography>
                <Button variant="contained" onClick={() => setOpen(true)}>
                    Create Template
                </Button>
            </Box>

            <Paper sx={{ width: '100%', overflow: 'hidden' }}>
                <TableContainer sx={{ maxHeight: 440 }}>
                    <Table stickyHeader>
                        <TableHead>
                            <TableRow>
                                <TableCell>Name</TableCell>
                                <TableCell>Format</TableCell>
                                <TableCell>Redemption Limit</TableCell>
                                <TableCell>Created</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {templates?.map((template: any) => (
                                <TableRow key={template.id} hover>
                                    <TableCell sx={{ fontWeight: 500 }}>{template.name}</TableCell>
                                    <TableCell><code>{template.codeFormat}</code></TableCell>
                                    <TableCell>{template.redemptionLimit?.limitOverall || 'Unlimited'}</TableCell>
                                    <TableCell color="text.secondary">
                                        {new Date(template.createdDate).toLocaleDateString()}
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </TableContainer>
            </Paper>

            <Dialog open={open} onClose={() => setOpen(false)}>
                <DialogTitle>New Template</DialogTitle>
                <DialogContent>
                    <TextField autoFocus margin="dense" label="Name" fullWidth value={name} onChange={(e) => setName(e.target.value)} />
                    <TextField margin="dense" label="Code Format (####=num, ????=alphanum)" fullWidth value={codeFormat} onChange={(e) => setCodeFormat(e.target.value)} />
                    <TextField margin="dense" label="Validity (Days)" type="number" fullWidth value={validityDays} onChange={(e) => setValidityDays(Number(e.target.value))} />
                    <TextField margin="dense" label="Redemption Limit" type="number" fullWidth value={redemptionLimit} onChange={(e) => setRedemptionLimit(Number(e.target.value))} />
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setOpen(false)}>Cancel</Button>
                    <Button onClick={handleSubmit} variant="contained">Create</Button>
                </DialogActions>
            </Dialog>
        </div>
    );
};
