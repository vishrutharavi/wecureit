import { signInWithEmailAndPassword, createUserWithEmailAndPassword } from "firebase/auth";
import { auth } from "./firebase";

export async function login(email: string, password: string) {
  const userCred = await signInWithEmailAndPassword(auth, email, password);
  return userCred.user;
}

export async function signup(email: string, password: string) {
  const userCred = await createUserWithEmailAndPassword(auth, email, password);
  return userCred.user;
}

