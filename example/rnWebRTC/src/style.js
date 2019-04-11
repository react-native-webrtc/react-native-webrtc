import { StyleSheet } from "react-native";


const s = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: "center",
    backgroundColor: "#F5FCFF",
  },
  rtcView: {
    flex: 1,
    width: '100%',
    marginTop: 10,
  },
  button: {
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'blue',
    borderRadius: 10,
    margin: 5,
    padding: 10,
  },
  buttonText: {
    fontSize: 20,
    color: 'white',
  },
});

export default s;