import { initializeApp, getApps } from "firebase/app";
import { getAuth } from "firebase/auth";

const firebaseConfig = {
  apiKey: "AIzaSyBOF42H98BborasAUfzl-UXC0blY56gHt4",
  authDomain: "wecureit.firebaseapp.com",
  projectId: "wecureit",
  storageBucket: "wecureit.firebasestorage.app",
  messagingSenderId: "143760587855",
  appId: "1:143760587855:web:45f6b45d2488d7f52ffe3a",
};

const app = getApps().length ? getApps()[0] : initializeApp(firebaseConfig);

export const auth = getAuth(app);
