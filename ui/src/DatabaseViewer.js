import React, { useState, useEffect } from 'react';
import {
  Container,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Tabs,
  Tab,
  Box,
  Card,
  CardContent,
  Grid,
  TextField,
  Button,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Pagination,
  IconButton,
  Tooltip
} from '@mui/material';
import {
  Refresh as RefreshIcon,
  Visibility as VisibilityIcon,
  Phone as PhoneIcon,
  Warning as WarningIcon,
  CheckCircle as CheckCircleIcon,
  Error as ErrorIcon
} from '@mui/icons-material';

const API_BASE_URL = 'http://localhost:8080/admin/database/api';

function TabPanel({ children, value, index, ...other }) {
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`database-tabpanel-${index}`}
      aria-labelledby={`database-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ pt: 3 }}>{children}</Box>}
    </div>
  );
}

function DatabaseViewer() {
  const [tabValue, setTabValue] = useState(0);
  const [incidents, setIncidents] = useState([]);
  const [voiceCalls, setVoiceCalls] = useState([]);
  const [statistics, setStatistics] = useState({});
  const [loading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedItem, setSelectedItem] = useState(null);
  const [detailDialogOpen, setDetailDialogOpen] = useState(false);
  const [pagination, setPagination] = useState({
    incidents: { page: 0, totalPages: 0, totalElements: 0 },
    voiceCalls: { page: 0, totalPages: 0, totalElements: 0 }
  });

  useEffect(() => {
    loadStatistics();
    loadIncidents();
    loadVoiceCalls();
  }, []);

  const loadStatistics = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/statistics`);
      const data = await response.json();
      setStatistics(data);
    } catch (error) {
      console.error('Error loading statistics:', error);
    }
  };

  const loadIncidents = async (page = 0) => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/incidents?page=${page}&size=20`);
      const data = await response.json();
      setIncidents(data.content || []);
      setPagination(prev => ({
        ...prev,
        incidents: {
          page: data.currentPage,
          totalPages: data.totalPages,
          totalElements: data.totalElements
        }
      }));
    } catch (error) {
      console.error('Error loading incidents:', error);
      setIncidents([]);
    }
    setLoading(false);
  };

  const loadVoiceCalls = async (page = 0) => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/voice-calls?page=${page}&size=20`);
      const data = await response.json();
      setVoiceCalls(data.content || []);
      setPagination(prev => ({
        ...prev,
        voiceCalls: {
          page: data.currentPage,
          totalPages: data.totalPages,
          totalElements: data.totalElements
        }
      }));
    } catch (error) {
      console.error('Error loading voice calls:', error);
      setVoiceCalls([]);
    }
    setLoading(false);
  };

  const searchIncidents = async () => {
    if (!searchQuery.trim()) {
      loadIncidents();
      return;
    }
    try {
      const response = await fetch(`${API_BASE_URL}/incidents/search?query=${encodeURIComponent(searchQuery)}`);
      const data = await response.json();
      setIncidents(data || []);
      setPagination(prev => ({
        ...prev,
        incidents: { page: 0, totalPages: 1, totalElements: data.length }
      }));
    } catch (error) {
      console.error('Error searching incidents:', error);
    }
  };

  const searchVoiceCalls = async () => {
    if (!searchQuery.trim()) {
      loadVoiceCalls();
      return;
    }
    try {
      const response = await fetch(`${API_BASE_URL}/voice-calls/search?query=${encodeURIComponent(searchQuery)}`);
      const data = await response.json();
      setVoiceCalls(data || []);
      setPagination(prev => ({
        ...prev,
        voiceCalls: { page: 0, totalPages: 1, totalElements: data.length }
      }));
    } catch (error) {
      console.error('Error searching voice calls:', error);
    }
  };

  const showDetails = async (type, id) => {
    try {
      const response = await fetch(`${API_BASE_URL}/${type}/${id}`);
      const data = await response.json();
      setSelectedItem({ type, data });
      setDetailDialogOpen(true);
    } catch (error) {
      console.error('Error loading details:', error);
    }
  };

  const handleTabChange = (event, newValue) => {
    setTabValue(newValue);
    setSearchQuery('');
  };

  const handleSearch = () => {
    if (tabValue === 0) {
      searchIncidents();
    } else {
      searchVoiceCalls();
    }
  };

  const handleRefresh = () => {
    setSearchQuery('');
    if (tabValue === 0) {
      loadIncidents();
    } else {
      loadVoiceCalls();
    }
    loadStatistics();
  };

  const formatDateTime = (dateTime) => {
    if (!dateTime) return 'N/A';
    return new Date(dateTime).toLocaleString();
  };

  const formatDuration = (seconds) => {
    if (!seconds) return 'N/A';
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return minutes > 0 ? `${minutes}:${remainingSeconds.toString().padStart(2, '0')}` : `${seconds}s`;
  };

  const getSeverityColor = (severity) => {
    switch (severity?.toLowerCase()) {
      case 'high': return 'error';
      case 'medium': return 'warning';
      case 'low': return 'success';
      default: return 'default';
    }
  };

  const getStatusColor = (status) => {
    switch (status?.toLowerCase()) {
      case 'resolved': return 'success';
      case 'processed': return 'success';
      case 'error': return 'error';
      case 'failed': return 'error';
      case 'in_progress': return 'info';
      case 'processing': return 'info';
      default: return 'default';
    }
  };

  return (
    <Container maxWidth="xl" sx={{ mt: 4 }}>
      <Typography variant="h4" gutterBottom>
        Database Viewer
      </Typography>

      {/* Statistics Cards */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box display="flex" justifyContent="space-between" alignItems="center">
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Total Incidents
                  </Typography>
                  <Typography variant="h4">
                    {statistics.incidents?.total || 0}
                  </Typography>
                </Box>
                <WarningIcon color="primary" sx={{ fontSize: 40 }} />
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box display="flex" justifyContent="space-between" alignItems="center">
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Voice Calls
                  </Typography>
                  <Typography variant="h4">
                    {statistics.voiceCalls?.total || 0}
                  </Typography>
                </Box>
                <PhoneIcon color="success" sx={{ fontSize: 40 }} />
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box display="flex" justifyContent="space-between" alignItems="center">
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Today's Incidents
                  </Typography>
                  <Typography variant="h4">
                    {statistics.incidents?.today || 0}
                  </Typography>
                </Box>
                <CheckCircleIcon color="info" sx={{ fontSize: 40 }} />
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box display="flex" justifyContent="space-between" alignItems="center">
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Today's Calls
                  </Typography>
                  <Typography variant="h4">
                    {statistics.voiceCalls?.today || 0}
                  </Typography>
                </Box>
                <PhoneIcon color="warning" sx={{ fontSize: 40 }} />
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Table Tabs */}
      <Paper sx={{ width: '100%' }}>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={tabValue} onChange={handleTabChange}>
            <Tab label="Incidents" />
            <Tab label="Voice Calls" />
          </Tabs>
        </Box>

        {/* Search and Controls */}
        <Box sx={{ p: 2, display: 'flex', gap: 2, alignItems: 'center' }}>
          <TextField
            label={tabValue === 0 ? "Search incidents..." : "Search by phone number..."}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
            size="small"
            sx={{ minWidth: 300 }}
          />
          <Button variant="outlined" onClick={handleSearch}>
            Search
          </Button>
          <Tooltip title="Refresh">
            <IconButton onClick={handleRefresh}>
              <RefreshIcon />
            </IconButton>
          </Tooltip>
        </Box>

        {/* Incidents Tab */}
        <TabPanel value={tabValue} index={0}>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>ID</TableCell>
                  <TableCell>External ID</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Severity</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Source</TableCell>
                  <TableCell>Description</TableCell>
                  <TableCell>Voice</TableCell>
                  <TableCell>Created</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={10} align="center">Loading...</TableCell>
                  </TableRow>
                ) : incidents.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={10} align="center">No incidents found</TableCell>
                  </TableRow>
                ) : (
                  incidents.map((incident) => (
                    <TableRow key={incident.id}>
                      <TableCell>{incident.id}</TableCell>
                      <TableCell sx={{ maxWidth: 150, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {incident.externalId}
                      </TableCell>
                      <TableCell>
                        <Chip label={incident.type} size="small" />
                      </TableCell>
                      <TableCell>
                        <Chip 
                          label={incident.severity} 
                          size="small" 
                          color={getSeverityColor(incident.severity)}
                        />
                      </TableCell>
                      <TableCell>
                        <Chip 
                          label={incident.status} 
                          size="small" 
                          color={getStatusColor(incident.status)}
                        />
                      </TableCell>
                      <TableCell>{incident.source || 'N/A'}</TableCell>
                      <TableCell sx={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {incident.description || 'N/A'}
                      </TableCell>
                      <TableCell>
                        {incident.conversationUuid && <PhoneIcon color="primary" />}
                      </TableCell>
                      <TableCell>{formatDateTime(incident.createdAt)}</TableCell>
                      <TableCell>
                        <Tooltip title="View Details">
                          <IconButton 
                            size="small" 
                            onClick={() => showDetails('incidents', incident.id)}
                          >
                            <VisibilityIcon />
                          </IconButton>
                        </Tooltip>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
          
          {pagination.incidents.totalPages > 1 && (
            <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}>
              <Pagination
                count={pagination.incidents.totalPages}
                page={pagination.incidents.page + 1}
                onChange={(e, page) => loadIncidents(page - 1)}
              />
            </Box>
          )}
        </TabPanel>

        {/* Voice Calls Tab */}
        <TabPanel value={tabValue} index={1}>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>ID</TableCell>
                  <TableCell>Conversation UUID</TableCell>
                  <TableCell>Caller Number</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Duration</TableCell>
                  <TableCell>Transcription</TableCell>
                  <TableCell>Service</TableCell>
                  <TableCell>Confidence</TableCell>
                  <TableCell>Created</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={10} align="center">Loading...</TableCell>
                  </TableRow>
                ) : voiceCalls.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={10} align="center">No voice calls found</TableCell>
                  </TableRow>
                ) : (
                  voiceCalls.map((call) => (
                    <TableRow key={call.id}>
                      <TableCell>{call.id}</TableCell>
                      <TableCell sx={{ maxWidth: 150, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {call.conversationUuid}
                      </TableCell>
                      <TableCell>{call.callerNumber || 'N/A'}</TableCell>
                      <TableCell>
                        <Chip 
                          label={call.processingStatus} 
                          size="small" 
                          color={getStatusColor(call.processingStatus)}
                        />
                      </TableCell>
                      <TableCell>{formatDuration(call.callDuration)}</TableCell>
                      <TableCell sx={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {call.transcription || 'N/A'}
                      </TableCell>
                      <TableCell>{call.speechToTextService || 'N/A'}</TableCell>
                      <TableCell>
                        {call.transcriptionConfidence ? 
                          `${(parseFloat(call.transcriptionConfidence) * 100).toFixed(1)}%` : 'N/A'}
                      </TableCell>
                      <TableCell>{formatDateTime(call.createdAt)}</TableCell>
                      <TableCell>
                        <Tooltip title="View Details">
                          <IconButton 
                            size="small" 
                            onClick={() => showDetails('voice-calls', call.id)}
                          >
                            <VisibilityIcon />
                          </IconButton>
                        </Tooltip>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
          
          {pagination.voiceCalls.totalPages > 1 && (
            <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}>
              <Pagination
                count={pagination.voiceCalls.totalPages}
                page={pagination.voiceCalls.page + 1}
                onChange={(e, page) => loadVoiceCalls(page - 1)}
              />
            </Box>
          )}
        </TabPanel>
      </Paper>

      {/* Detail Dialog */}
      <Dialog 
        open={detailDialogOpen} 
        onClose={() => setDetailDialogOpen(false)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>
          {selectedItem?.type === 'incidents' ? 'Incident Details' : 'Voice Call Details'}
        </DialogTitle>
        <DialogContent>
          {selectedItem && (
            <Box sx={{ mt: 2 }}>
              {selectedItem.type === 'incidents' ? (
                <IncidentDetails incident={selectedItem.data} />
              ) : (
                <VoiceCallDetails voiceCall={selectedItem.data} />
              )}
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDetailDialogOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
}

function IncidentDetails({ incident }) {
  const formatDateTime = (dateTime) => {
    if (!dateTime) return 'N/A';
    return new Date(dateTime).toLocaleString();
  };

  return (
    <Grid container spacing={3}>
      <Grid item xs={12} md={6}>
        <Typography variant="h6" gutterBottom>Basic Information</Typography>
        <Table size="small">
          <TableBody>
            <TableRow>
              <TableCell><strong>ID:</strong></TableCell>
              <TableCell>{incident.id}</TableCell>
            </TableRow>
            <TableRow>
              <TableCell><strong>External ID:</strong></TableCell>
              <TableCell>{incident.externalId}</TableCell>
            </TableRow>
            <TableRow>
              <TableCell><strong>Type:</strong></TableCell>
              <TableCell>{incident.type}</TableCell>
            </TableRow>
            <TableRow>
              <TableCell><strong>Severity:</strong></TableCell>
              <TableCell>{incident.severity}</TableCell>
            </TableRow>
            <TableRow>
              <TableCell><strong>Status:</strong></TableCell>
              <TableCell>{incident.status}</TableCell>
            </TableRow>
            <TableRow>
              <TableCell><strong>Source:</strong></TableCell>
              <TableCell>{incident.source || 'N/A'}</TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Grid>
      <Grid item xs={12} md={6}>
        <Typography variant="h6" gutterBottom>Timestamps</Typography>
        <Table size="small">
          <TableBody>
            <TableRow>
              <TableCell><strong>Created:</strong></TableCell>
              <TableCell>{formatDateTime(incident.createdAt)}</TableCell>
            </TableRow>
            <TableRow>
              <TableCell><strong>Updated:</strong></TableCell>
              <TableCell>{formatDateTime(incident.updatedAt)}</TableCell>
            </TableRow>
            <TableRow>
              <TableCell><strong>Resolved:</strong></TableCell>
              <TableCell>{formatDateTime(incident.resolvedAt)}</TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Grid>
      <Grid item xs={12}>
        <Typography variant="h6" gutterBottom>Description</Typography>
        <Paper sx={{ p: 2, bgcolor: 'grey.50' }}>
          {incident.description || 'No description available'}
        </Paper>
      </Grid>
      {incident.transcription && (
        <Grid item xs={12}>
          <Typography variant="h6" gutterBottom>Voice Transcription</Typography>
          <Paper sx={{ p: 2, bgcolor: 'info.light', color: 'info.contrastText' }}>
            {incident.transcription}
          </Paper>
        </Grid>
      )}
      {incident.aiSuggestion && (
        <Grid item xs={12}>
          <Typography variant="h6" gutterBottom>AI Suggestion</Typography>
          <Paper sx={{ p: 2, bgcolor: 'success.light', color: 'success.contrastText' }}>
            {incident.aiSuggestion}
          </Paper>
        </Grid>
      )}
    </Grid>
  );
}

function VoiceCallDetails({ voiceCall }) {
  const formatDateTime = (dateTime) => {
    if (!dateTime) return 'N/A';
    return new Date(dateTime).toLocaleString();
  };

  const formatDuration = (seconds) => {
    if (!seconds) return 'N/A';
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return minutes > 0 ? `${minutes}:${remainingSeconds.toString().padStart(2, '0')}` : `${seconds}s`;
  };

  return (
    <Grid container spacing={3}>
      <Grid item xs={12} md={6}>
        <Typography variant="h6" gutterBottom>Call Information</Typography>
        <Table size="small">
          <TableBody>
            <TableRow>
              <TableCell><strong>ID:</strong></TableCell>
              <TableCell>{voiceCall.id}</TableCell>
            </TableRow>
            <TableRow>
              <TableCell><strong>Conversation UUID:</strong></TableCell>
              <TableCell>{voiceCall.conversationUuid}</TableCell>
            </TableRow>
            <TableRow>
              <TableCell><strong>Caller Number:</strong></TableCell>
              <TableCell>{voiceCall.callerNumber || 'N/A'}</TableCell>
            </TableRow>
            <TableRow>
              <TableCell><strong>Duration:</strong></TableCell>
              <TableCell>{formatDuration(voiceCall.callDuration)}</TableCell>
            </TableRow>
            <TableRow>
              <TableCell><strong>Status:</strong></TableCell>
              <TableCell>{voiceCall.processingStatus}</TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Grid>
      <Grid item xs={12} md={6}>
        <Typography variant="h6" gutterBottom>Processing Details</Typography>
        <Table size="small">
          <TableBody>
            <TableRow>
              <TableCell><strong>Service:</strong></TableCell>
              <TableCell>{voiceCall.speechToTextService || 'N/A'}</TableCell>
            </TableRow>
            <TableRow>
              <TableCell><strong>Confidence:</strong></TableCell>
              <TableCell>
                {voiceCall.transcriptionConfidence ? 
                  `${(parseFloat(voiceCall.transcriptionConfidence) * 100).toFixed(1)}%` : 'N/A'}
              </TableCell>
            </TableRow>
            <TableRow>
              <TableCell><strong>Created:</strong></TableCell>
              <TableCell>{formatDateTime(voiceCall.createdAt)}</TableCell>
            </TableRow>
            <TableRow>
              <TableCell><strong>Processed:</strong></TableCell>
              <TableCell>{formatDateTime(voiceCall.processedAt)}</TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Grid>
      {voiceCall.transcription && (
        <Grid item xs={12}>
          <Typography variant="h6" gutterBottom>Transcription</Typography>
          <Paper sx={{ p: 2, bgcolor: 'grey.50' }}>
            {voiceCall.transcription}
          </Paper>
        </Grid>
      )}
      {voiceCall.errorMessage && (
        <Grid item xs={12}>
          <Typography variant="h6" gutterBottom>Error Message</Typography>
          <Paper sx={{ p: 2, bgcolor: 'error.light', color: 'error.contrastText' }}>
            {voiceCall.errorMessage}
          </Paper>
        </Grid>
      )}
      {voiceCall.recordingUrl && (
        <Grid item xs={12}>
          <Typography variant="h6" gutterBottom>Recording</Typography>
          <Button 
            variant="outlined" 
            href={voiceCall.recordingUrl} 
            target="_blank"
            startIcon={<PhoneIcon />}
          >
            Play Recording
          </Button>
        </Grid>
      )}
    </Grid>
  );
}

export default DatabaseViewer;