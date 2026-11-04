import React from "react";
import { createRoot } from "react-dom/client";
import "../generated/tokens.css";

// Aşama 0: tasarım ekranı ve gerçek tarayıcı henüz oluşturulmadı.
function EymenRoot() {
  return <div style={{
    width: "100vw", height: "100vh",
    background: "var(--renk-gece-zemin)"
  }} />;
}

const root = document.getElementById("root");
if (root) createRoot(root).render(<EymenRoot />);
