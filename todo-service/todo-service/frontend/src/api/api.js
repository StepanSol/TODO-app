import axios from "axios";

const api = (keycloak) => {
  const instance = axios.create({
    baseURL: "http://localhost:8081",
  });

  instance.interceptors.request.use((config) => {
    if (keycloak?.token) {
      config.headers.Authorization = `Bearer ${keycloak.token}`;
    }
    return config;
  });

  return instance;
};

export default api;