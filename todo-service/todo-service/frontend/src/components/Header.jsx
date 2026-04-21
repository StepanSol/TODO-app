import { useNavigate } from "react-router-dom";
import { isAdmin } from "../auth/roles";

function Header({ keycloak }) {
  const navigate = useNavigate();

  const username = keycloak?.tokenParsed?.preferred_username;

  const logout = () => {
    keycloak.logout({
      redirectUri: "http://localhost:3001"
    });
  };

  return (
    <div style={styles.header}>
      <div style={styles.logo} onClick={() => navigate("/")}>
        Todo App
      </div>

      <div style={styles.right}>
        <span>👤 {username}</span>

          {isAdmin(keycloak) && (
            <button
              style={styles.adminBtn}
              onClick={() => navigate("/admin")}
            >
              Admin
            </button>
          )}

        <button style={styles.logoutBtn} onClick={logout}>
          Logout
        </button>
      </div>
    </div>
  );
}

const styles = {
  header: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    padding: "15px 30px",
    background: "#1e1e2f",
    color: "white"
  },
  logo: {
    fontWeight: "bold",
    cursor: "pointer"
  },
  right: {
    display: "flex",
    alignItems: "center",
    gap: "15px"
  },
  adminBtn: {
    padding: "6px 12px",
    background: "#4CAF50",
    border: "none",
    color: "white",
    borderRadius: "6px",
    cursor: "pointer"
  },
  logoutBtn: {
    padding: "6px 12px",
    background: "#e53935",
    border: "none",
    color: "white",
    borderRadius: "6px",
    cursor: "pointer"
  }
};

export default Header;