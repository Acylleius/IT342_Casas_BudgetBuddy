import { useEffect, useState } from "react";

const STORAGE_KEY = "budgetbuddy_web_session";
const transactionTypeOptions = ["EXPENSE", "INCOME"];
const categoryOptions = [
  "Food",
  "Transportation",
  "Bills",
  "Shopping",
  "Health",
  "Education",
  "Salary",
  "Freelance",
  "Savings",
  "Other"
];

const emptyLoginForm = {
  email: "",
  password: ""
};

const emptyRegisterForm = {
  firstname: "",
  lastname: "",
  email: "",
  password: ""
};

const emptyTransactionForm = {
  type: "EXPENSE",
  amount: "",
  category: "Food",
  description: "",
  transactionDate: new Date().toISOString().slice(0, 10)
};

function readStoredSession() {
  const rawSession = localStorage.getItem(STORAGE_KEY);

  if (!rawSession) {
    return null;
  }

  try {
    return JSON.parse(rawSession);
  } catch {
    localStorage.removeItem(STORAGE_KEY);
    return null;
  }
}

async function request(path, options = {}) {
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? "";
  const response = await fetch(`${apiBaseUrl}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...(options.headers ?? {})
    },
    ...options
  });

  const body = await response.json().catch(() => null);

  if (!response.ok || (body && body.success === false)) {
    const message =
      body?.error?.details ||
      body?.error?.message ||
      body?.message ||
      "Request failed. Please try again.";

    throw new Error(message);
  }

  return body;
}

function formatCurrency(value) {
  return new Intl.NumberFormat("en-PH", {
    style: "currency",
    currency: "PHP"
  }).format(Number(value ?? 0));
}

function formatDate(value) {
  return new Intl.DateTimeFormat("en-PH", {
    month: "short",
    day: "numeric",
    year: "numeric"
  }).format(new Date(value));
}

export default function App() {
  const [mode, setMode] = useState("login");
  const [session, setSession] = useState(() => readStoredSession());
  const [loginForm, setLoginForm] = useState(emptyLoginForm);
  const [registerForm, setRegisterForm] = useState(emptyRegisterForm);
  const [transactionForm, setTransactionForm] = useState(emptyTransactionForm);
  const [transactions, setTransactions] = useState([]);
  const [summary, setSummary] = useState({
    totalIncome: 0,
    totalExpense: 0,
    balance: 0,
    transactionCount: 0
  });
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoadingTransactions, setIsLoadingTransactions] = useState(false);
  const [deletingId, setDeletingId] = useState(null);

  useEffect(() => {
    if (session) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    } else {
      localStorage.removeItem(STORAGE_KEY);
    }
  }, [session]);

  useEffect(() => {
    if (!session?.user?.id) {
      setTransactions([]);
      setSummary({
        totalIncome: 0,
        totalExpense: 0,
        balance: 0,
        transactionCount: 0
      });
      return;
    }

    fetchTransactions(session.user.id);
  }, [session?.user?.id]);

  const displayName =
    [session?.user?.firstname, session?.user?.lastname]
      .filter(Boolean)
      .join(" ")
      .trim() || session?.user?.email || "BudgetBuddy User";

  async function fetchTransactions(userId) {
    setIsLoadingTransactions(true);

    try {
      const result = await request(`/api/v1/transactions?userId=${userId}`);
      setTransactions(result.data.transactions);
      setSummary(result.data.summary);
    } catch (error) {
      setErrorMessage(error.message);
    } finally {
      setIsLoadingTransactions(false);
    }
  }

  async function handleLoginSubmit(event) {
    event.preventDefault();
    setErrorMessage("");
    setSuccessMessage("");
    setIsSubmitting(true);

    try {
      const result = await request("/api/v1/auth/login", {
        method: "POST",
        body: JSON.stringify(loginForm)
      });

      setSession({
        user: result.data.user,
        accessToken: result.data.accessToken,
        refreshToken: result.data.refreshToken
      });
      setSuccessMessage(result.message);
      setLoginForm(emptyLoginForm);
    } catch (error) {
      setErrorMessage(error.message);
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleRegisterSubmit(event) {
    event.preventDefault();
    setErrorMessage("");
    setSuccessMessage("");
    setIsSubmitting(true);

    try {
      const result = await request("/api/v1/auth/register", {
        method: "POST",
        body: JSON.stringify(registerForm)
      });

      setRegisterForm(emptyRegisterForm);
      setMode("login");
      setSuccessMessage(`${result.message} You can log in now.`);
    } catch (error) {
      setErrorMessage(error.message);
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleTransactionSubmit(event) {
    event.preventDefault();
    setErrorMessage("");
    setSuccessMessage("");
    setIsSubmitting(true);

    try {
      const payload = {
        ...transactionForm,
        userId: session.user.id,
        amount: Number(transactionForm.amount)
      };

      const result = await request("/api/v1/transactions", {
        method: "POST",
        body: JSON.stringify(payload)
      });

      setSuccessMessage(result.message);
      setTransactionForm({
        ...emptyTransactionForm,
        transactionDate: emptyTransactionForm.transactionDate
      });
      await fetchTransactions(session.user.id);
    } catch (error) {
      setErrorMessage(error.message);
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleDeleteTransaction(transactionId) {
    setErrorMessage("");
    setSuccessMessage("");
    setDeletingId(transactionId);

    try {
      const result = await request(
        `/api/v1/transactions/${transactionId}?userId=${session.user.id}`,
        { method: "DELETE" }
      );
      setSuccessMessage(result.message);
      await fetchTransactions(session.user.id);
    } catch (error) {
      setErrorMessage(error.message);
    } finally {
      setDeletingId(null);
    }
  }

  function handleLogout() {
    setSession(null);
    setTransactions([]);
    setSuccessMessage("You have been signed out.");
    setErrorMessage("");
  }

  function renderAuthForm() {
    return (
      <div className="auth-card">
        <div className="tab-row" role="tablist" aria-label="Authentication mode">
          <button
            className={mode === "login" ? "tab active" : "tab"}
            type="button"
            onClick={() => {
              setMode("login");
              setErrorMessage("");
            }}
          >
            Login
          </button>
          <button
            className={mode === "register" ? "tab active" : "tab"}
            type="button"
            onClick={() => {
              setMode("register");
              setErrorMessage("");
            }}
          >
            Register
          </button>
        </div>

        <div className="card-copy">
          <h2>{mode === "login" ? "Welcome back" : "Create your account"}</h2>
          <p>
            {mode === "login"
              ? "Sign in to start tracking expenses and income in your web dashboard."
              : "Create a BudgetBuddy account connected to your existing backend database."}
          </p>
        </div>

        {errorMessage ? <div className="message error">{errorMessage}</div> : null}
        {successMessage ? <div className="message success">{successMessage}</div> : null}

        {mode === "login" ? (
          <form className="auth-form" onSubmit={handleLoginSubmit}>
            <label>
              Email
              <input
                type="email"
                value={loginForm.email}
                onChange={(event) =>
                  setLoginForm((current) => ({
                    ...current,
                    email: event.target.value
                  }))
                }
                placeholder="you@example.com"
                required
              />
            </label>
            <label>
              Password
              <input
                type="password"
                value={loginForm.password}
                onChange={(event) =>
                  setLoginForm((current) => ({
                    ...current,
                    password: event.target.value
                  }))
                }
                placeholder="Enter your password"
                required
              />
            </label>
            <button className="primary-button" type="submit" disabled={isSubmitting}>
              {isSubmitting ? "Signing in..." : "Sign in"}
            </button>
          </form>
        ) : (
          <form className="auth-form" onSubmit={handleRegisterSubmit}>
            <label>
              First name
              <input
                type="text"
                value={registerForm.firstname}
                onChange={(event) =>
                  setRegisterForm((current) => ({
                    ...current,
                    firstname: event.target.value
                  }))
                }
                placeholder="Juan"
                required
              />
            </label>
            <label>
              Last name
              <input
                type="text"
                value={registerForm.lastname}
                onChange={(event) =>
                  setRegisterForm((current) => ({
                    ...current,
                    lastname: event.target.value
                  }))
                }
                placeholder="Dela Cruz"
                required
              />
            </label>
            <label>
              Email
              <input
                type="email"
                value={registerForm.email}
                onChange={(event) =>
                  setRegisterForm((current) => ({
                    ...current,
                    email: event.target.value
                  }))
                }
                placeholder="you@example.com"
                required
              />
            </label>
            <label>
              Password
              <input
                type="password"
                value={registerForm.password}
                onChange={(event) =>
                  setRegisterForm((current) => ({
                    ...current,
                    password: event.target.value
                  }))
                }
                placeholder="Create a password"
                required
              />
            </label>
            <button className="primary-button" type="submit" disabled={isSubmitting}>
              {isSubmitting ? "Creating account..." : "Create account"}
            </button>
          </form>
        )}
      </div>
    );
  }

  function renderDashboard() {
    return (
      <div className="dashboard-layout">
        <div className="dashboard-card wide">
          <div className="dashboard-header">
            <div>
              <p className="eyebrow">Budget Dashboard</p>
              <h2>{displayName}</h2>
              <p className="muted">
                Add income and expenses to keep a running picture of your finances.
              </p>
            </div>
            <button className="ghost-button" type="button" onClick={handleLogout}>
              Log out
            </button>
          </div>

          {errorMessage ? <div className="message error">{errorMessage}</div> : null}
          {successMessage ? <div className="message success">{successMessage}</div> : null}

          <div className="summary-grid">
            <article className="summary-card">
              <p>Total Income</p>
              <strong>{formatCurrency(summary.totalIncome)}</strong>
            </article>
            <article className="summary-card">
              <p>Total Expense</p>
              <strong>{formatCurrency(summary.totalExpense)}</strong>
            </article>
            <article className="summary-card">
              <p>Balance</p>
              <strong>{formatCurrency(summary.balance)}</strong>
            </article>
            <article className="summary-card">
              <p>Records</p>
              <strong>{summary.transactionCount}</strong>
            </article>
          </div>
        </div>

        <div className="dashboard-card">
          <div className="card-copy compact">
            <h2>Add transaction</h2>
            <p>Record an expense or income entry and save it directly to the database.</p>
          </div>

          <form className="auth-form" onSubmit={handleTransactionSubmit}>
            <label>
              Type
              <select
                value={transactionForm.type}
                onChange={(event) =>
                  setTransactionForm((current) => ({
                    ...current,
                    type: event.target.value
                  }))
                }
              >
                {transactionTypeOptions.map((option) => (
                  <option key={option} value={option}>
                    {option}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Amount
              <input
                type="number"
                min="0.01"
                step="0.01"
                value={transactionForm.amount}
                onChange={(event) =>
                  setTransactionForm((current) => ({
                    ...current,
                    amount: event.target.value
                  }))
                }
                placeholder="0.00"
                required
              />
            </label>

            <label>
              Category
              <select
                value={transactionForm.category}
                onChange={(event) =>
                  setTransactionForm((current) => ({
                    ...current,
                    category: event.target.value
                  }))
                }
              >
                {categoryOptions.map((option) => (
                  <option key={option} value={option}>
                    {option}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Date
              <input
                type="date"
                value={transactionForm.transactionDate}
                onChange={(event) =>
                  setTransactionForm((current) => ({
                    ...current,
                    transactionDate: event.target.value
                  }))
                }
                required
              />
            </label>

            <label>
              Description
              <textarea
                value={transactionForm.description}
                onChange={(event) =>
                  setTransactionForm((current) => ({
                    ...current,
                    description: event.target.value
                  }))
                }
                placeholder="Optional note for this entry"
                rows="3"
              />
            </label>

            <button className="primary-button" type="submit" disabled={isSubmitting}>
              {isSubmitting ? "Saving..." : "Save transaction"}
            </button>
          </form>
        </div>

        <div className="dashboard-card wide">
          <div className="card-copy compact">
            <h2>Recent transactions</h2>
            <p>Your saved budget activity is listed here from newest to oldest.</p>
          </div>

          {isLoadingTransactions ? (
            <div className="empty-state">Loading transactions...</div>
          ) : transactions.length === 0 ? (
            <div className="empty-state">
              No transactions yet. Add your first expense or income entry.
            </div>
          ) : (
            <div className="transaction-list">
              {transactions.map((transaction) => (
                <article className="transaction-row" key={transaction.id}>
                  <div className="transaction-main">
                    <div className="transaction-title-row">
                      <h3>{transaction.category}</h3>
                      <span
                        className={
                          transaction.type === "INCOME" ? "pill income" : "pill expense"
                        }
                      >
                        {transaction.type}
                      </span>
                    </div>
                    <p>{transaction.description || "No description provided."}</p>
                    <small>{formatDate(transaction.transactionDate)}</small>
                  </div>

                  <div className="transaction-side">
                    <strong>{formatCurrency(transaction.amount)}</strong>
                    <button
                      className="danger-button"
                      type="button"
                      onClick={() => handleDeleteTransaction(transaction.id)}
                      disabled={deletingId === transaction.id}
                    >
                      {deletingId === transaction.id ? "Deleting..." : "Delete"}
                    </button>
                  </div>
                </article>
              ))}
            </div>
          )}
        </div>
      </div>
    );
  }

  return (
    <div className="app-shell">
      <section className="hero-panel">
        <p className="eyebrow">BudgetBuddy Web</p>
        <h1>Track the money that moves your day.</h1>
        <p className="hero-copy">
          BudgetBuddy focuses on the core job of a budgeting system: recording transactions,
          separating income from expenses, and showing the balance clearly enough to act on it.
        </p>

        <div className="feature-grid">
          <article className="feature-card">
            <span>01</span>
            <h2>Authentication</h2>
            <p>Users can register, log in, and stay signed in across refreshes.</p>
          </article>
          <article className="feature-card">
            <span>02</span>
            <h2>Main feature</h2>
            <p>Income and expense transactions are saved to the backend database.</p>
          </article>
          <article className="feature-card">
            <span>03</span>
            <h2>Usable output</h2>
            <p>Totals and recent entries update immediately after each action.</p>
          </article>
        </div>
      </section>

      <section className="surface-panel">
        {session ? renderDashboard() : renderAuthForm()}
      </section>
    </div>
  );
}
