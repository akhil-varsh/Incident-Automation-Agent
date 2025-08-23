import React, { useState, useEffect } from 'react';
import {
  Card,
  CardContent,
  Typography,
  TextField,
  Button,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Box,
  Alert,
  CircularProgress,
  Chip,
  Grid
} from '@mui/material';
import { Phone, PhoneCallback } from '@mui/icons-material';

const OUTBOUND_API_URL = 'http://localhost:8080/api/twilio/outbound';
const INCIDENTS_API_URL = 'http://localhost:8080/api/v1/incidents';

function OutboundCallPanel() {
  const [incidents, setIncidents] = useState([]);
  const [selectedIncident, setSelectedIncident] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);

  useEffect(() => {
    fetchIncidents();
  }, []);

  const fetchIncidents = async () => {
    try {
      const response = await fetch(INCIDENTS_API_URL);
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const data = await response.json();
      console.log('Raw incidents data:', data);

      const incidentList = Array.isArray(data) ? data : (data?.content || []);
      console.log('Processed incident list:', incidentList);
      
      if (incidentList.length > 0) {
        console.log('First incident keys:', Object.keys(incidentList[0]));
        console.log('First incident full object:', incidentList[0]);
      }

      setIncidents(incidentList);

    } catch (error) {
      console.error('Failed to fetch incidents:', error);
      setResult({
        success: false,
        message: `Failed to fetch incidents: ${error.message}`
      });
    }
  };

  const makeOutboundCall = async () => {
    if (!phoneNumber.trim() || !selectedIncident) {
      setResult({
        success: false,
        message: 'Please select an incident and enter a phone number'
      });
      return;
    }

    setLoading(true);
    setResult(null);

    try {
      const incident = incidents.find(inc => getIncidentId(inc) === selectedIncident);
      if (!incident) {
        throw new Error('Selected incident not found');
      }

      const payload = {
        toPhoneNumber: phoneNumber.trim(),
        incidentId: getIncidentId(incident),
        severity: incident.severity || 'MEDIUM',
        message: `Incident ${getIncidentId(incident)}: ${incident.description || 'Requires attention'}`
      };

      const response = await fetch(`${OUTBOUND_API_URL}/call/incident-notification`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
      });

      const data = await response.json();

      if (response.ok && data.success) {
        setResult({
          success: true,
          message: `Call initiated successfully! Call SID: ${data.callSid}`,
          callSid: data.callSid
        });
      } else {
        setResult({
          success: false,
          message: data.message || `Failed to initiate call (HTTP ${response.status})`
        });
      }
    } catch (error) {
      console.error('Outbound call error:', error);
      setResult({
        success: false,
        message: `Error: ${error.message}`
      });
    } finally {
      setLoading(false);
    }
  };

  const getSeverityColor = (severity) => {
    switch (severity?.toUpperCase()) {
      case 'CRITICAL': return 'error';
      case 'HIGH': return 'warning';
      case 'MEDIUM': return 'info';
      case 'LOW': return 'success';
      default: return 'default';
    }
  };

  // Helper function to get incident identifier
  const getIncidentId = (incident) => {
    return incident.id || incident.incidentId || incident.externalId || incident.internalId || `incident-${incidents.indexOf(incident)}`;
  };

  return (
    <Card sx={{ mt: 3 }}>
      <CardContent>
        <Typography variant="h5" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <PhoneCallback color="primary" />
          Outbound Call Center
        </Typography>

        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>

              {/* Incident Selection */}
              <FormControl fullWidth required>
                <InputLabel>Select Incident</InputLabel>
                <Select
                  value={selectedIncident || ''}
                  onChange={(e) => {
                    console.log('Select onChange:', e.target.value);
                    setSelectedIncident(e.target.value || '');
                  }}
                  label="Select Incident"
                  displayEmpty
                >
                  <MenuItem value="">
                    <em>Choose an incident...</em>
                  </MenuItem>
                  {incidents.map((incident) => (
                    <MenuItem key={getIncidentId(incident)} value={getIncidentId(incident)}>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, width: '100%' }}>
                        <Typography variant="body2" sx={{ fontWeight: 'bold' }}>
                          {getIncidentId(incident)}
                        </Typography>
                        <Chip
                          label={incident.severity || 'MEDIUM'}
                          size="small"
                          color={getSeverityColor(incident.severity)}
                        />
                        <Typography variant="body2" sx={{ ml: 1, flexGrow: 1 }}>
                          {incident.type || 'Unknown Type'}
                        </Typography>
                      </Box>
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>

              {/* Phone Number Input */}
              <TextField
                label="Phone Number"
                value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value)}
                placeholder="+1234567890"
                required
                fullWidth
                helperText="Include country code (e.g., +1 for US)"
              />

              {/* Call Button */}
              <Button
                variant="contained"
                color="primary"
                size="large"
                startIcon={loading ? <CircularProgress size={20} /> : <Phone />}
                onClick={makeOutboundCall}
                disabled={loading || !phoneNumber.trim() || !selectedIncident}
                fullWidth
              >
                {loading ? 'Initiating Call...' : 'Make Outbound Call'}
              </Button>

            </Box>
          </Grid>

          <Grid item xs={12} md={6}>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>

              {/* Call Preview */}
              <Typography variant="h6" gutterBottom>
                Call Preview
              </Typography>

              {selectedIncident && incidents.find(inc => getIncidentId(inc) === selectedIncident) && (
                <Card variant="outlined">
                  <CardContent>
                    {(() => {
                      const incident = incidents.find(inc => getIncidentId(inc) === selectedIncident);
                      return (
                        <>
                          <Typography variant="subtitle1" gutterBottom>
                            <strong>Incident:</strong> {getIncidentId(incident)}
                          </Typography>
                          <Typography variant="body2" gutterBottom>
                            <strong>Type:</strong> {incident.type || 'Unknown'}
                          </Typography>
                          <Typography variant="body2" gutterBottom>
                            <strong>Severity:</strong>
                            <Chip
                              label={incident.severity || 'MEDIUM'}
                              size="small"
                              color={getSeverityColor(incident.severity)}
                              sx={{ ml: 1 }}
                            />
                          </Typography>
                          <Typography variant="body2" gutterBottom>
                            <strong>Description:</strong> {incident.description || 'No description'}
                          </Typography>
                          <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
                            <strong>Call Message:</strong><br />
                            "Incident {getIncidentId(incident)}: {incident.description || 'Requires attention'}"
                          </Typography>
                        </>
                      );
                    })()}
                  </CardContent>
                </Card>
              )}

              {/* Result Display */}
              {result && (
                <Alert severity={result.success ? 'success' : 'error'}>
                  <Typography variant="body2">
                    {result.message}
                  </Typography>
                  {result.success && result.callSid && (
                    <Typography variant="caption" display="block" sx={{ mt: 1 }}>
                      Call SID: {result.callSid}
                    </Typography>
                  )}
                </Alert>
              )}

              {/* Debug Info */}
              <Card variant="outlined" sx={{ p: 2, bgcolor: 'grey.50' }}>
                <Typography variant="subtitle2" gutterBottom>
                  Status
                </Typography>
                <Typography variant="caption" display="block">
                  Incidents loaded: {incidents.length}
                </Typography>
                <Typography variant="caption" display="block">
                  Selected: {selectedIncident || 'None'}
                </Typography>
                <Typography variant="caption" display="block">
                  Phone: {phoneNumber || 'None'}
                </Typography>
                <Typography variant="caption" display="block">
                  API Endpoint: {OUTBOUND_API_URL}/call/incident-notification
                </Typography>
                {incidents.length > 0 && (
                  <Typography variant="caption" display="block" sx={{ mt: 1 }}>
                    First incident: ID={incidents[0].id}, Type={incidents[0].type}
                  </Typography>
                )}
                <Button
                  size="small"
                  onClick={() => {
                    console.log('All incidents:', incidents);
                    console.log('Selected incident:', selectedIncident);
                    incidents.forEach((inc, idx) => {
                      console.log(`Incident ${idx}:`, inc);
                      console.log(`Incident ${idx} keys:`, Object.keys(inc));
                    });
                  }}
                  sx={{ mt: 1 }}
                >
                  Debug Log
                </Button>
                {incidents.length > 0 && (
                  <Button
                    size="small"
                    variant="outlined"
                    onClick={() => {
                      console.log('Manually setting first incident:', incidents[0].id);
                      setSelectedIncident(incidents[0].id);
                    }}
                    sx={{ mt: 1, ml: 1 }}
                  >
                    Select First
                  </Button>
                )}
              </Card>

            </Box>
          </Grid>
        </Grid>
      </CardContent>
    </Card>
  );
}

export default OutboundCallPanel;