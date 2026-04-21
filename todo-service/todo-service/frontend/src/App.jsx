import { BrowserRouter, Routes, Route } from "react-router-dom";
import TodosPage from "./pages/TodosPage";
import Header from "./components/Header";
import AdminPage from "./pages/AdminPage";
import { isAdmin } from "./auth/roles";

function App({ keycloak }) {
  return (
    <BrowserRouter>
      <Header keycloak={keycloak} />

      <Routes>
        <Route path="/" element={<TodosPage keycloak={keycloak} />} />
        <Route path="/admin" element={<AdminPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;