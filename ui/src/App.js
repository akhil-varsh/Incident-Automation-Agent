import React, { useState, useEffect } from 'react';
import { Container, Typography, TextField, Button, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, Tabs, Tab, Box } from '@mui/material';
import NavBar from './NavBar';
import OutboundCallPanel from './OutboundCallPanel';
import DatabaseViewer from './DatabaseViewer';

const API_URL = 'http://localhost:8080/api/v1/incidents';

function TabPanel({ children, value, index, ...other }) {
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`simple-tabpanel-${index}`}
      aria-labelledby={`simple-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ pt: 3 }}>{children}</Box>}
    </div>
  );
}

function App() {
  const [incidents, setIncidents] = useState([]);
  const [form, setForm] = useState({ id: '', title: '', description: '', type: '', severity: '', source: '' });
  const [loading, setLoading] = useState(false);
  const [tabValue, setTabValue] = useState(0);

  useEffect(() => {
    fetchIncidents();
  }, []);

  const fetchIncidents = async () => {
    setLoading(true);
    try {
      const res = await fetch(API_URL);
      const data = await res.json();
      setIncidents(Array.isArray(data) ? data : data.content || []);
    } catch (e) {
      setIncidents([]);
    }
    setLoading(false);
  };

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value || '' });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.id || !form.title || !form.description || !form.type || !form.severity || !form.source) return;
    const payload = {
      id: form.id,
      type: form.type,
      description: form.description,
      severity: form.severity,
      source: form.source,
      timestamp: new Date().toISOString().slice(0, 19),
    };
    try {
      await fetch(API_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      setForm({ id: '', title: '', description: '', type: '', severity: '', source: '' });
      fetchIncidents();
    } catch (e) {
      // handle error
    }
  };

  const handleTabChange = (event, newValue) => {
    setTabValue(newValue);
  };

  return (
    <>
      <NavBar />
      <Container maxWidth="lg" style={{ marginTop: 32 }}>
        <Typography variant="h4" gutterBottom>Incident Management System</Typography>
        
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={tabValue} onChange={handleTabChange} aria-label="incident management tabs">
            <Tab label="Manage Incidents" />
            <Tab label="Outbound Calls" />
            <Tab label="Database Viewer" />
          </Tabs>
        </Box>

        <TabPanel value={tabValue} index={0}>
          <form onSubmit={handleSubmit} style={{ marginBottom: 32 }}>
            <TextField label="Incident ID" name="id" value={form.id} onChange={handleChange} required fullWidth margin="normal" />
            <TextField label="Title" name="title" value={form.title} onChange={handleChange} required fullWidth margin="normal" />
            <TextField label="Description" name="description" value={form.description} onChange={handleChange} required fullWidth margin="normal" multiline rows={2} />
            <TextField label="Type" name="type" value={form.type} onChange={handleChange} required fullWidth margin="normal" />
            <TextField label="Severity" name="severity" value={form.severity} onChange={handleChange} required fullWidth margin="normal" />
            <TextField label="Source" name="source" value={form.source} onChange={handleChange} required fullWidth margin="normal" />
            <Button type="submit" variant="contained" color="primary" style={{ marginTop: 16 }}>Submit Incident</Button>
          </form>
          
          <Typography variant="h6" gutterBottom>All Incidents</Typography>
          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>ID</TableCell>
                  <TableCell>Title</TableCell>
                  <TableCell>Description</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Severity</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {loading ? (
                  <TableRow><TableCell colSpan={5}>Loading...</TableCell></TableRow>
                ) : incidents.length === 0 ? (
                  <TableRow><TableCell colSpan={5}>No incidents found.</TableCell></TableRow>
                ) : (
                  incidents.map((inc, idx) => (
                    <TableRow key={idx}>
                      <TableCell>{inc.id || ''}</TableCell>
                      <TableCell>{inc.title || ''}</TableCell>
                      <TableCell>{inc.description || ''}</TableCell>
                      <TableCell>{inc.type || ''}</TableCell>
                      <TableCell>{inc.severity || ''}</TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </TabPanel>

        <TabPanel value={tabValue} index={1}>
          <OutboundCallPanel />
        </TabPanel>

        <TabPanel value={tabValue} index={2}>
          <DatabaseViewer />
        </TabPanel>
      </Container>
    </>
  );
}

export default App;
