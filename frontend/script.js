const API = "http://localhost:8080/api";

const state = { events: [], organizers: [], participants: [] };

// ---------- helpers ----------
const $ = (id) => document.getElementById(id);

function esc(value) {
  const d = document.createElement("div");
  d.textContent = value ?? "";
  return d.innerHTML;
}

const fmtDate = (d) => (d ? new Date(d + "T00:00:00").toLocaleDateString(undefined, { day: "numeric", month: "short", year: "numeric" }) : "-");
// Today as YYYY-MM-DD in the user's local time (also used as the minimum for the date picker)
const todayString = () => new Date().toLocaleDateString("en-CA");
const isExpired = (eventDate) => eventDate < todayString();

const fmtDateTime = (d) => (d ? new Date(d).toLocaleString() : "-");

let messageTimer;
function showMessage(text, isError = false) {
  const box = $("message");
  box.textContent = text;
  box.className = "message" + (isError ? " error" : "");
  box.hidden = false;
  clearTimeout(messageTimer);
  messageTimer = setTimeout(() => (box.hidden = true), 5000);
}

async function api(path, options = {}) {
  const res = await fetch(API + path, { headers: { "Content-Type": "application/json" }, ...options });
  if (res.status === 204) return null;
  const body = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(body.message || `Request failed (${res.status})`);
  return body;
}

async function run(action) {
  try {
    await action();
  } catch (err) {
    showMessage(
      err.message.includes("Failed to fetch") ? "Cannot reach the API. Is the backend running on port 8080?" : err.message,
      true
    );
  }
}

function options(list, label) {
  return list.map((item) => `<option value="${item.id}">${esc(label(item))}</option>`).join("");
}

// ---------- tabs ----------
document.querySelectorAll(".tab").forEach((btn) =>
  btn.addEventListener("click", () => {
    document.querySelectorAll(".tab").forEach((b) => b.classList.toggle("active", b === btn));
    document.querySelectorAll(".panel").forEach((p) => p.classList.toggle("active", p.id === btn.dataset.tab));
    if (btn.dataset.tab === "registrations") run(loadRegistrations);
    if (btn.dataset.tab === "reports") run(loadReports);
  })
);

// ---------- organizers ----------
async function loadOrganizers() {
  state.organizers = await api("/organizers");
  $("organizerRows").innerHTML =
    state.organizers
      .map(
        (o) => `<tr><td>${esc(o.name)}</td><td>${esc(o.email)}</td><td>${esc(o.phone || "-")}</td>
          <td class="actions"><button class="small danger" data-delete="${o.id}">Delete</button></td></tr>`
      )
      .join("") || `<tr><td class="empty" colspan="4">No organizers yet. Add one above.</td></tr>`;
  $("eventOrganizer").innerHTML = options(state.organizers, (o) => o.name);
}

$("organizerForm").addEventListener("submit", (e) => {
  e.preventDefault();
  run(async () => {
    await api("/organizers", {
      method: "POST",
      body: JSON.stringify({
        name: $("organizerName").value.trim(),
        email: $("organizerEmail").value.trim(),
        phone: $("organizerPhone").value.trim(),
      }),
    });
    $("organizerForm").reset();
    showMessage("Organizer added");
    await loadOrganizers();
  });
});

$("organizerRows").addEventListener("click", (e) => {
  const id = e.target.dataset.delete;
  if (id && confirm("Delete this organizer?")) {
    run(async () => {
      await api(`/organizers/${id}`, { method: "DELETE" });
      showMessage("Organizer deleted");
      await loadOrganizers();
    });
  }
});

// ---------- participants ----------
async function loadParticipants() {
  state.participants = await api("/participants");
  $("participantRows").innerHTML =
    state.participants
      .map(
        (p) => `<tr><td>${esc(p.name)}</td><td>${esc(p.email)}</td><td>${esc(p.phone || "-")}</td>
          <td class="actions"><button class="small danger" data-delete="${p.id}">Delete</button></td></tr>`
      )
      .join("") || `<tr><td class="empty" colspan="4">No participants yet. Add one above.</td></tr>`;
  $("registerParticipant").innerHTML = options(state.participants, (p) => `${p.name} (${p.email})`);
}

$("participantForm").addEventListener("submit", (e) => {
  e.preventDefault();
  run(async () => {
    await api("/participants", {
      method: "POST",
      body: JSON.stringify({
        name: $("participantName").value.trim(),
        email: $("participantEmail").value.trim(),
        phone: $("participantPhone").value.trim(),
      }),
    });
    $("participantForm").reset();
    showMessage("Participant added");
    await loadParticipants();
  });
});

$("participantRows").addEventListener("click", (e) => {
  const id = e.target.dataset.delete;
  if (id && confirm("Delete this participant?")) {
    run(async () => {
      await api(`/participants/${id}`, { method: "DELETE" });
      showMessage("Participant deleted");
      await loadParticipants();
    });
  }
});

// ---------- events ----------
async function loadEvents() {
  state.events = await api("/events");
  $("eventRows").innerHTML =
    state.events
      .map((ev) => {
        const taken = ev.totalSeats - ev.availableSeats;
        const full = ev.availableSeats === 0;
        return `<tr>
          <td><strong>${esc(ev.title)}</strong><br><small>${esc(ev.description)}</small></td>
          <td>${fmtDate(ev.eventDate)}${isExpired(ev.eventDate) ? ' <span class="badge">Expired</span>' : ""}</td>
          <td>${esc(ev.venue)}</td>
          <td>${esc(ev.organizerName)}</td>
          <td class="seats${full ? " full" : ""}">
            <strong>${full ? "Full" : ev.availableSeats + " of " + ev.totalSeats}</strong>
            <progress max="${ev.totalSeats}" value="${taken}"></progress>
          </td>
          <td class="num">${ev.registrationCount}</td>
          <td class="actions"><button class="small danger" data-delete="${ev.id}">Delete</button></td>
        </tr>`;
      })
      .join("") || `<tr><td class="empty" colspan="7">No events yet. Add one above.</td></tr>`;

  const eventOptions = options(state.events, (ev) =>
    isExpired(ev.eventDate) ? `${ev.title} (expired)` : `${ev.title} (${ev.availableSeats} seats left)`
  );
  $("registerEvent").innerHTML = eventOptions;
  $("countEvent").innerHTML = options(state.events, (ev) => ev.title);
  $("registrationFilter").innerHTML = `<option value="">All events</option>` + options(state.events, (ev) => ev.title);
}

$("eventForm").addEventListener("submit", (e) => {
  e.preventDefault();
  if (isExpired($("eventDate").value)) {
    showMessage("Event date cannot be in the past", true);
    return;
  }
  run(async () => {
    await api("/events", {
      method: "POST",
      body: JSON.stringify({
        title: $("eventTitle").value.trim(),
        description: $("eventDescription").value.trim(),
        eventDate: $("eventDate").value,
        venue: $("eventVenue").value.trim(),
        totalSeats: Number($("eventSeats").value),
        organizerId: Number($("eventOrganizer").value),
      }),
    });
    $("eventForm").reset();
    showMessage("Event added");
    await loadEvents();
  });
});

$("eventRows").addEventListener("click", (e) => {
  const id = e.target.dataset.delete;
  if (id && confirm("Delete this event?")) {
    run(async () => {
      await api(`/events/${id}`, { method: "DELETE" });
      showMessage("Event deleted");
      await loadEvents();
    });
  }
});

// ---------- registrations ----------
async function loadRegistrations() {
  const eventId = $("registrationFilter").value;
  const rows = await api("/registrations" + (eventId ? `?eventId=${eventId}` : ""));
  $("registrationRows").innerHTML =
    rows
      .map(
        (r) => `<tr>
          <td>${r.registrationId}</td>
          <td>${esc(r.participantName)}</td>
          <td>${esc(r.participantEmail)}</td>
          <td>${esc(r.eventTitle)}</td>
          <td>${fmtDate(r.eventDate)}</td>
          <td>${esc(r.venue)}</td>
          <td>${fmtDateTime(r.registeredAt)}</td>
          <td class="actions"><button class="small danger" data-cancel="${r.registrationId}">Cancel</button></td>
        </tr>`
      )
      .join("") || `<tr><td class="empty" colspan="8">No registrations to show.</td></tr>`;
}

$("registerForm").addEventListener("submit", (e) => {
  e.preventDefault();
  run(async () => {
    const result = await api("/registrations", {
      method: "POST",
      body: JSON.stringify({
        eventId: Number($("registerEvent").value),
        participantId: Number($("registerParticipant").value),
      }),
    });
    showMessage(result.message);
    await Promise.all([loadRegistrations(), loadEvents()]);
  });
});

$("registrationFilter").addEventListener("change", () => run(loadRegistrations));

$("registrationRows").addEventListener("click", (e) => {
  const id = e.target.dataset.cancel;
  if (id && confirm("Cancel this registration?")) {
    run(async () => {
      await api(`/registrations/${id}`, { method: "DELETE" });
      showMessage("Registration cancelled");
      await Promise.all([loadRegistrations(), loadEvents()]);
    });
  }
});

// ---------- reports ----------
async function loadReports() {
  const events = await api("/events/above-average-registrations");
  $("aboveAverageList").innerHTML =
    events.map((ev) => `<li>${esc(ev.title)} — ${ev.registrationCount} registrations</li>`).join("") ||
    `<li class="empty">No event is above the average yet.</li>`;
}

$("countForm").addEventListener("submit", (e) => {
  e.preventDefault();
  run(async () => {
    const data = await api(`/events/${$("countEvent").value}/registration-count`);
    $("countResult").textContent = `${data.title}: ${data.registrationCount} registration(s)`;
  });
});

// ---------- start ----------
$("eventDate").min = todayString(); // past dates cannot be picked
run(async () => {
  await Promise.all([loadOrganizers(), loadParticipants()]);
  await loadEvents();
});
