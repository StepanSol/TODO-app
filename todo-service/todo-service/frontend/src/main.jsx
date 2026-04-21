import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App.jsx";
import keycloak from "./auth/keycloak";

keycloak
  .init({
    onLoad: "login-required",
    pkceMethod: "S256"
  })
  .then((authenticated) => {
    if (!authenticated) {
      window.location.reload();
      return;
    }

    ReactDOM.createRoot(document.getElementById("root")).render(
      <React.StrictMode>
        <App keycloak={keycloak} />
      </React.StrictMode>
    );
  });