import React, { useEffect, useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';

import { Result, runSelfTest, summarize } from './selfTest';

const COLORS = { pass: '#2e7d32', fail: '#c62828', skip: '#757575' };

// Runs the self-test on launch and shows the results.
const App = () => {
  const [results, setResults] = useState<Result[]>([]);
  const [running, setRunning] = useState<string | undefined>('starting');
  const [summary, setSummary] = useState<string | null>(null);

  useEffect(() => {
    runSelfTest((partial, current) => {
      setResults(partial);
      setRunning(current);
    })
      .then((all) => setSummary(summarize(all)))
      .catch((e) => setSummary(`SELFTEST CRASHED ${e?.message ?? e}`));
  }, []);

  return (
    <View style={styles.screen}>
      <ScrollView contentContainerStyle={styles.container}>
        <Text style={styles.title}>Daily WebRTC self-test</Text>
        <Text style={styles.summary}>{summary ?? `Running: ${running}`}</Text>
        {results.map((r) => (
          <View key={r.name} style={styles.row}>
            <Text style={[styles.status, { color: COLORS[r.status] }]}>
              {r.status.toUpperCase()}
            </Text>
            <View style={styles.text}>
              <Text>{r.name}</Text>
              {r.detail ? <Text style={styles.detail}>{r.detail}</Text> : null}
            </View>
          </View>
        ))}
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  screen: { flex: 1, paddingTop: 48, backgroundColor: '#ffffff' },
  container: { padding: 16 },
  title: { fontSize: 20, fontWeight: 'bold', marginBottom: 8 },
  summary: { fontSize: 16, marginBottom: 16 },
  row: { flexDirection: 'row', marginBottom: 12 },
  status: { width: 48, fontWeight: 'bold' },
  text: { flex: 1 },
  detail: { color: COLORS.skip, fontSize: 12 },
});

export default App;
