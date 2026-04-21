export const isAdmin = (keycloak) => {
  const roles = keycloak?.tokenParsed?.realm_access?.roles || [];

  return roles.includes("ADMIN");
};