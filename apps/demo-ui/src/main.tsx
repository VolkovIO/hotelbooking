import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import "./styles/global.css";

/**
 * React application entry point.
 *
 * index.html contains:
 *
 *   <div id="root"></div>
 *
 * React finds this element and renders our App component inside it.
 */
ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);