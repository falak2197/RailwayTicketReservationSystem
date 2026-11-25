(() => {
    "use strict";

    // Application state synchronized exclusively with the Java backend REST API
    const state = {
        trains: [],
        displayedTrains: [],
        trainsFilterActive: false,
        trainsViewMode: "cards",
        selectedTrainForDetails: null,
        selectedJourneyDate: "",
        passengers: [createEmptyPassenger()],
        pendingCancelPnr: null,
        currentCancelBooking: null
    };

    const API_BASE = "/api";

    // =======================================================
    // Initialization
    // =======================================================
    document.addEventListener("DOMContentLoaded", () => {
        initializeDates();
        bindNavigation();
        bindHomeEvents();
        bindTrainsEvents();
        bindTrainDetailsEvents();
        bindBookingEvents();
        bindPnrEvents();
        bindCancelEvents();
        bindBookingsEvents();
        bindContactEvents();

        renderPassengerRows();
        loadInitialData();
        handleInitialHash();
    });

    function createEmptyPassenger() {
        return { name: "", age: "", gender: "Male", phoneNumber: "" };
    }

    function initializeDates() {
        const today = new Date().toISOString().split("T")[0];
        state.selectedJourneyDate = today;

        const heroDate = document.getElementById("heroDate");
        const bookDate = document.getElementById("bookJourneyDate");
        [heroDate, bookDate].forEach((el) => {
            if (el) {
                el.min = today;
                el.value = today;
            }
        });
    }

    async function loadInitialData() {
        await Promise.all([refreshTrains(), refreshHomeMetrics()]);
        populateTrainSelect();
        populateStationDatalists();
        renderDynamicRouteChips();
    }

    // =======================================================
    // HTTP API Helper
    // =======================================================
    async function apiRequest(path, options = {}) {
        const response = await fetch(`${API_BASE}${path}`, {
            headers: { "Content-Type": "application/json", ...(options.headers || {}) },
            ...options
        });

        let data = null;
        const text = await response.text();
        if (text) {
            try {
                data = JSON.parse(text);
            } catch (_) {
                data = null;
            }
        }

        if (!response.ok) {
            const message = (data && data.error) ? data.error : `Request failed (HTTP ${response.status})`;
            throw new Error(message);
        }

        setBackendOnline(true);
        return data;
    }

    function setBackendOnline(online) {
        const pill = document.getElementById("backendStatusPill");
        if (!pill) return;
        pill.classList.remove("online", "offline");
        pill.classList.add(online ? "online" : "offline");
        pill.innerHTML = `<span class="status-dot"></span> ${
            online ? "Connected to Java Backend" : "Java Backend Unreachable"
        }`;
    }

    // =======================================================
    // Navigation & Routing
    // =======================================================
    function bindNavigation() {
        const navMenu = document.getElementById("navMenu");
        const navToggle = document.getElementById("navToggle");

        document.querySelectorAll("[data-nav]").forEach((el) => {
            el.addEventListener("click", (e) => {
                e.preventDefault();
                const section = el.getAttribute("data-nav");
                navigateTo(section);
                if (navMenu) navMenu.classList.remove("open");
                if (navToggle) navToggle.setAttribute("aria-expanded", "false");
            });
        });

        document.querySelectorAll("[data-nav-card]").forEach((card) => {
            card.addEventListener("click", () => {
                navigateTo(card.getAttribute("data-nav-card"));
            });
        });

        if (navToggle && navMenu) {
            navToggle.addEventListener("click", () => {
                const isOpen = navMenu.classList.toggle("open");
                navToggle.setAttribute("aria-expanded", String(isOpen));
            });
        }

        window.addEventListener("hashchange", handleInitialHash);
    }

    function handleInitialHash() {
        const hash = window.location.hash.replace("#", "").trim();
        const validSections = ["home", "trains", "train-details", "book", "cancel", "pnr", "bookings", "contact"];
        if (validSections.includes(hash)) {
            navigateTo(hash, false);
        }
    }

    function navigateTo(section, updateHash = true) {
        const targetEl = document.getElementById(`section-${section}`);
        if (!targetEl) return;

        document.querySelectorAll(".page-section").forEach((sec) => sec.classList.remove("active"));
        targetEl.classList.add("active");

        document.querySelectorAll(".nav-link").forEach((link) => {
            const target = link.getAttribute("data-nav");
            const isMatch = target === section || (section === "train-details" && target === "trains");
            link.classList.toggle("active", isMatch);
        });

        if (updateHash) {
            window.location.hash = section;
        }
        window.scrollTo({ top: 0, behavior: "smooth" });

        // Fetch fresh data when entering key sections
        if (section === "trains") {
            if (!state.trainsFilterActive || state.displayedTrains.length === 0) {
                loadAllTrains();
            } else {
                refreshTrains().then(() => renderTrains());
            }
        } else if (section === "book") {
            refreshTrains().then(() => {
                populateTrainSelect();
                updateFarePreview();
            });
        } else if (section === "bookings") {
            loadAllBookings();
        } else if (section === "home") {
            refreshHomeMetrics();
        }
    }

    // =======================================================
    // Home Page Events
    // =======================================================
    function bindHomeEvents() {
        const swapBtn = document.getElementById("swapStationsBtn");
        const heroSource = document.getElementById("heroSource");
        const heroDestination = document.getElementById("heroDestination");
        const heroForm = document.getElementById("heroSearchForm");
        const quickPnrForm = document.getElementById("homeQuickPnrForm");

        if (swapBtn) {
            swapBtn.addEventListener("click", () => {
                const temp = heroSource.value;
                heroSource.value = heroDestination.value;
                heroDestination.value = temp;
            });
        }

        if (heroForm) {
            heroForm.addEventListener("submit", async (e) => {
                e.preventDefault();
                const source = heroSource.value.trim();
                const destination = heroDestination.value.trim();
                const journeyDate = document.getElementById("heroDate").value;

                if (journeyDate) {
                    state.selectedJourneyDate = journeyDate;
                    const bookDate = document.getElementById("bookJourneyDate");
                    if (bookDate) bookDate.value = journeyDate;
                }

                document.getElementById("trainsSourceInput").value = source;
                document.getElementById("trainsDestInput").value = destination;
                navigateTo("trains");
                await searchTrainsByRoute(source, destination);
            });
        }

        if (quickPnrForm) {
            quickPnrForm.addEventListener("submit", async (e) => {
                e.preventDefault();
                const pnr = document.getElementById("homeQuickPnrInput").value.trim();
                document.getElementById("pnrInput").value = pnr;
                navigateTo("pnr");
                await lookupPnrStatus(pnr);
            });
        }
    }

    async function refreshTrains() {
        try {
            const trains = await apiRequest("/trains");
            state.trains = Array.isArray(trains) ? trains : [];
            if (!state.trainsFilterActive || state.displayedTrains.length === 0) {
                state.displayedTrains = [...state.trains];
            } else {
                state.displayedTrains = state.displayedTrains.map((oldTrain) => {
                    const updated = state.trains.find((t) => t.trainNumber === oldTrain.trainNumber);
                    return updated || oldTrain;
                });
            }
            return state.trains;
        } catch (err) {
            setBackendOnline(false);
            showToast(err.message, "error");
            return [];
        }
    }

    async function refreshHomeMetrics() {
        try {
            const [trains, bookings] = await Promise.all([
                apiRequest("/trains"),
                apiRequest("/bookings")
            ]);
            state.trains = trains;
            const totalSeatsAvailable = trains.reduce((sum, t) => sum + t.availableSeats, 0);

            document.getElementById("heroTrainCount").textContent = String(trains.length);
            document.getElementById("heroSeatCount").textContent = String(totalSeatsAvailable);
            document.getElementById("heroBookingCount").textContent = String(bookings.length);
        } catch (_) {
            setBackendOnline(false);
        }
    }

    function populateStationDatalists() {
        const sources = [...new Set(state.trains.map((t) => t.source))];
        const destinations = [...new Set(state.trains.map((t) => t.destination))];

        const sourceList = document.getElementById("sourceOptions");
        const destList = document.getElementById("destinationOptions");

        if (sourceList) {
            sourceList.innerHTML = sources.map((s) => `<option value="${escapeHtml(s)}"></option>`).join("");
        }
        if (destList) {
            destList.innerHTML = destinations.map((d) => `<option value="${escapeHtml(d)}"></option>`).join("");
        }
    }

    function renderDynamicRouteChips() {
        const container = document.getElementById("dynamicRouteChips");
        if (!container) return;

        if (!state.trains.length) {
            container.innerHTML = `<span class="chip-loading">No routes loaded</span>`;
            return;
        }

        container.innerHTML = state.trains
            .map((t) => `
                <button type="button" class="route-chip" data-source="${escapeHtml(t.source)}" data-dest="${escapeHtml(t.destination)}">
                    ${escapeHtml(t.source)} &rarr; ${escapeHtml(t.destination)}
                </button>
            `)
            .join("");

        container.querySelectorAll(".route-chip").forEach((chip) => {
            chip.addEventListener("click", async () => {
                const source = chip.getAttribute("data-source");
                const dest = chip.getAttribute("data-dest");
                document.getElementById("heroSource").value = source;
                document.getElementById("heroDestination").value = dest;
                document.getElementById("trainsSourceInput").value = source;
                document.getElementById("trainsDestInput").value = dest;
                navigateTo("trains");
                await searchTrainsByRoute(source, dest);
            });
        });
    }

    // =======================================================
    // B. Trains & Search Trains
    // =======================================================
    function bindTrainsEvents() {
        document.getElementById("showAllTrainsBtn").addEventListener("click", loadAllTrains);

        document.getElementById("trainsRouteSearchForm").addEventListener("submit", async (e) => {
            e.preventDefault();
            const source = document.getElementById("trainsSourceInput").value.trim();
            const dest = document.getElementById("trainsDestInput").value.trim();
            await searchTrainsByRoute(source, dest);
        });

        document.getElementById("trainNumberSearchForm").addEventListener("submit", async (e) => {
            e.preventDefault();
            const num = document.getElementById("trainNumberInput").value.trim();
            await findTrainByNumberInList(num);
        });

        document.getElementById("viewCardsBtn").addEventListener("click", () => {
            state.trainsViewMode = "cards";
            document.getElementById("viewCardsBtn").classList.add("active");
            document.getElementById("viewTableBtn").classList.remove("active");
            renderTrains();
        });

        document.getElementById("viewTableBtn").addEventListener("click", () => {
            state.trainsViewMode = "table";
            document.getElementById("viewTableBtn").classList.add("active");
            document.getElementById("viewCardsBtn").classList.remove("active");
            renderTrains();
        });
    }

    async function loadAllTrains() {
        try {
            state.trainsFilterActive = false;
            const trains = await apiRequest("/trains");
            state.trains = trains;
            state.displayedTrains = trains;
            document.getElementById("trainsSubtitle").textContent =
                "Showing all trains available in the Railway Reservation system.";
            renderTrains();
        } catch (err) {
            showToast(err.message, "error");
        }
    }

    async function searchTrainsByRoute(source, destination) {
        try {
            state.trainsFilterActive = true;
            const params = new URLSearchParams({ source, destination });
            const trains = await apiRequest(`/trains/search?${params.toString()}`);
            state.displayedTrains = trains;
            document.getElementById("trainsSubtitle").textContent =
                `Search results for route: ${source} → ${destination}${
                    state.selectedJourneyDate ? ` (${state.selectedJourneyDate})` : ""
                }`;
            renderTrains();
        } catch (err) {
            state.displayedTrains = [];
            renderTrains(err.message);
        }
    }

    async function findTrainByNumberInList(trainNumber) {
        try {
            state.trainsFilterActive = true;
            const train = await apiRequest(`/trains/${encodeURIComponent(trainNumber)}`);
            state.displayedTrains = [train];
            document.getElementById("trainsSubtitle").textContent = `Showing train #${train.trainNumber}`;
            renderTrains();
        } catch (err) {
            state.displayedTrains = [];
            renderTrains(err.message);
        }
    }

    function renderTrains(emptyMessage) {
        const container = document.getElementById("trainsContainer");
        const countEl = document.getElementById("trainsResultCount");
        const list = state.displayedTrains;

        countEl.textContent = `${list.length} train${list.length === 1 ? "" : "s"} found`;

        if (!list.length) {
            container.className = "";
            container.innerHTML = `
                <div class="card empty-state">
                    <h3>No Matching Trains Found</h3>
                    <p>${escapeHtml(emptyMessage || "No trains matched your search criteria. Try viewing all available trains.")}</p>
                    <button type="button" class="btn btn-primary" id="emptyResetTrainsBtn">Display All Trains</button>
                </div>
            `;
            document.getElementById("emptyResetTrainsBtn").addEventListener("click", loadAllTrains);
            return;
        }

        if (state.trainsViewMode === "table") {
            container.className = "";
            container.innerHTML = `
                <div class="table-wrapper">
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Train No.</th>
                                <th>Train Name</th>
                                <th>Source</th>
                                <th>Destination</th>
                                <th>Seats (Avail / Total)</th>
                                <th>Fare / Seat</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${list.map((t) => {
                                const badge = getSeatBadge(t);
                                return `
                                    <tr>
                                        <td><strong>#${t.trainNumber}</strong></td>
                                        <td>${escapeHtml(t.trainName)}</td>
                                        <td>${escapeHtml(t.source)}</td>
                                        <td>${escapeHtml(t.destination)}</td>
                                        <td>
                                            <strong>${t.availableSeats}</strong> / ${t.totalSeats}
                                            <span class="badge ${badge.cls}" style="margin-left:6px;">${badge.label}</span>
                                        </td>
                                        <td><strong>${formatCurrency(t.farePerSeat)}</strong></td>
                                        <td>
                                            <div class="table-actions">
                                                <button type="button" class="btn btn-outline btn-sm" data-view-train="${t.trainNumber}">View Details</button>
                                                <button type="button" class="btn btn-primary btn-sm" data-book-train="${t.trainNumber}" ${t.availableSeats === 0 ? "disabled" : ""}>Book</button>
                                            </div>
                                        </td>
                                    </tr>
                                `;
                            }).join("")}
                        </tbody>
                    </table>
                </div>
            `;
        } else {
            container.className = "trains-grid";
            container.innerHTML = list.map((t) => {
                const badge = getSeatBadge(t);
                const pct = t.totalSeats > 0 ? Math.round((t.availableSeats / t.totalSeats) * 100) : 0;
                const fillClass = t.availableSeats === 0 ? "full" : pct <= 25 ? "low" : "";
                return `
                    <article class="train-card">
                        <div class="train-card-top">
                            <div>
                                <span class="train-num-badge">Train #${t.trainNumber}</span>
                                <h3>${escapeHtml(t.trainName)}</h3>
                            </div>
                            <div class="fare-tag">
                                <strong>${formatCurrency(t.farePerSeat)}</strong>
                                <span>per seat</span>
                            </div>
                        </div>

                        <div class="route-banner">
                            <div class="station-point">
                                <span>Source</span>
                                <strong>${escapeHtml(t.source)}</strong>
                            </div>
                            <div class="route-arrow">&bull;&mdash;&rarr;</div>
                            <div class="station-point" style="text-align:right;">
                                <span>Destination</span>
                                <strong>${escapeHtml(t.destination)}</strong>
                            </div>
                        </div>

                        <div class="seat-progress-block">
                            <div class="seat-meta">
                                <span>Available Seats: <strong>${t.availableSeats} / ${t.totalSeats}</strong></span>
                                <span class="badge ${badge.cls}">${badge.label}</span>
                            </div>
                            <div class="progress-track">
                                <div class="progress-fill ${fillClass}" style="width: ${pct}%"></div>
                            </div>
                        </div>

                        <div class="train-card-actions">
                            <button type="button" class="btn btn-outline" data-view-train="${t.trainNumber}">View Details</button>
                            <button type="button" class="btn btn-primary" data-book-train="${t.trainNumber}" ${t.availableSeats === 0 ? "disabled" : ""}>
                                ${t.availableSeats === 0 ? "Train Full" : "Book Ticket"}
                            </button>
                        </div>
                    </article>
                `;
            }).join("");
        }

        container.querySelectorAll("[data-view-train]").forEach((btn) => {
            btn.addEventListener("click", () => openTrainDetails(Number(btn.getAttribute("data-view-train"))));
        });

        container.querySelectorAll("[data-book-train]").forEach((btn) => {
            btn.addEventListener("click", () => startBookingForTrain(Number(btn.getAttribute("data-book-train"))));
        });
    }

    function getSeatBadge(train) {
        if (train.availableSeats === 0) return { cls: "badge-full", label: "Full" };
        if (train.availableSeats <= Math.max(10, Math.floor(train.totalSeats * 0.2))) {
            return { cls: "badge-limited", label: "Filling Fast" };
        }
        return { cls: "badge-available", label: "Available" };
    }

    // =======================================================
    // C. Train Details
    // =======================================================
    function bindTrainDetailsEvents() {
        document.getElementById("detailLookupForm").addEventListener("submit", async (e) => {
            e.preventDefault();
            const num = document.getElementById("detailLookupInput").value.trim();
            if (num) {
                await openTrainDetails(Number(num));
            }
        });
    }

    async function openTrainDetails(trainNumber) {
        navigateTo("train-details");
        const container = document.getElementById("trainDetailsContainer");
        container.innerHTML = `<div class="card empty-state"><p>Fetching live train &amp; seat availability...</p></div>`;

        try {
            const [train, availability] = await Promise.all([
                apiRequest(`/trains/${encodeURIComponent(trainNumber)}`),
                apiRequest(`/trains/${encodeURIComponent(trainNumber)}/availability`)
            ]);
            state.selectedTrainForDetails = train;
            document.getElementById("detailLookupInput").value = String(train.trainNumber);

            const badge = getSeatBadge(train);
            const pct = availability.totalSeats > 0
                ? Math.round((availability.availableSeats / availability.totalSeats) * 100)
                : 0;

            container.innerHTML = `
                <div class="card details-hero-card">
                    <div class="details-top">
                        <div>
                            <span class="train-num-badge">Train #${train.trainNumber}</span>
                            <h2 style="font-size:1.65rem; color:var(--navy-900); margin-top:4px;">${escapeHtml(train.trainName)}</h2>
                            <p style="color:var(--slate-600); margin-top:4px;">
                                Route: <strong>${escapeHtml(train.source)}</strong> &rarr; <strong>${escapeHtml(train.destination)}</strong>
                            </p>
                        </div>
                        <div style="text-align:right;">
                            <span class="badge ${badge.cls}" style="font-size:0.85rem; padding:6px 14px;">${badge.label}</span>
                            <div style="margin-top:8px; font-size:1.35rem; font-weight:800; color:var(--orange-600);">
                                ${formatCurrency(train.farePerSeat)} <span style="font-size:0.82rem; font-weight:500; color:var(--slate-500);">/ seat</span>
                            </div>
                        </div>
                    </div>

                    <div class="details-grid">
                        <div class="detail-stat-box">
                            <span>Total Capacity</span>
                            <strong>${availability.totalSeats} Seats</strong>
                        </div>
                        <div class="detail-stat-box">
                            <span>Remaining Seats</span>
                            <strong style="color:var(--green-700);">${availability.availableSeats} Seats</strong>
                        </div>
                        <div class="detail-stat-box">
                            <span>Booked Seats</span>
                            <strong>${availability.bookedSeats} Seats</strong>
                        </div>
                        <div class="detail-stat-box">
                            <span>Fare Per Seat</span>
                            <strong>${formatCurrency(train.farePerSeat)}</strong>
                        </div>
                    </div>

                    <div class="seat-progress-block" style="margin-bottom:24px;">
                        <div class="seat-meta">
                            <span>Remaining Seat Ratio</span>
                            <strong>${pct}% Available</strong>
                        </div>
                        <div class="progress-track" style="height:10px;">
                            <div class="progress-fill ${train.availableSeats === 0 ? "full" : pct <= 25 ? "low" : ""}" style="width:${pct}%"></div>
                        </div>
                    </div>

                    <div class="header-actions">
                        <button type="button" class="btn btn-primary btn-lg" id="detailBookNowBtn" ${train.availableSeats === 0 ? "disabled" : ""}>
                            ${train.availableSeats === 0 ? "No Seats Remaining" : "Book Tickets on This Train"}
                        </button>
                        <button type="button" class="btn btn-outline btn-lg" id="detailRefreshBtn">Refresh Availability</button>
                    </div>
                </div>
            `;

            document.getElementById("detailBookNowBtn").addEventListener("click", () => startBookingForTrain(train.trainNumber));
            document.getElementById("detailRefreshBtn").addEventListener("click", () => openTrainDetails(train.trainNumber));
        } catch (err) {
            container.innerHTML = `
                <div class="card empty-state">
                    <h3>Train Not Found</h3>
                    <p>${escapeHtml(err.message)}</p>
                    <button type="button" class="btn btn-primary" data-nav="trains">Browse All Trains</button>
                </div>
            `;
            container.querySelector("[data-nav='trains']").addEventListener("click", () => navigateTo("trains"));
        }
    }

    // =======================================================
    // D. Book Tickets
    // =======================================================
    function bindBookingEvents() {
        const trainSelect = document.getElementById("bookTrainSelect");
        const addPassengerBtn = document.getElementById("addPassengerBtn");
        const bookingForm = document.getElementById("bookingForm");
        const bookDate = document.getElementById("bookJourneyDate");

        trainSelect.addEventListener("change", () => {
            updateSelectedTrainBanner();
            updateFarePreview();
        });

        bookDate.addEventListener("change", () => {
            state.selectedJourneyDate = bookDate.value;
        });

        addPassengerBtn.addEventListener("click", () => {
            state.passengers.push(createEmptyPassenger());
            renderPassengerRows();
            updateSelectedTrainBanner();
            updateFarePreview();
        });

        bookingForm.addEventListener("submit", handleBookingSubmit);
    }

    function startBookingForTrain(trainNumber) {
        navigateTo("book");
        const confirmationPanel = document.getElementById("bookingConfirmationPanel");
        const workspace = document.getElementById("bookingWorkspace");
        confirmationPanel.classList.add("hidden");
        workspace.classList.remove("hidden");

        refreshTrains().then(() => {
            populateTrainSelect(trainNumber);
            updateSelectedTrainBanner();
            updateFarePreview();
        });
    }

    function populateTrainSelect(preferredTrainNumber) {
        const select = document.getElementById("bookTrainSelect");
        const currentValue = preferredTrainNumber ? String(preferredTrainNumber) : select.value;

        select.innerHTML = `<option value="">-- Select a Train --</option>` +
            state.trains.map((t) => `
                <option value="${t.trainNumber}">
                    #${t.trainNumber} — ${escapeHtml(t.trainName)} (${escapeHtml(t.source)} → ${escapeHtml(t.destination)}) [${t.availableSeats} seats left | ${formatCurrency(t.farePerSeat)}]
                </option>
            `).join("");

        if (currentValue && state.trains.some((t) => String(t.trainNumber) === currentValue)) {
            select.value = currentValue;
        }
        updateSelectedTrainBanner();
    }

    function updateSelectedTrainBanner() {
        const select = document.getElementById("bookTrainSelect");
        const banner = document.getElementById("selectedTrainBanner");
        const train = state.trains.find((t) => String(t.trainNumber) === select.value);

        if (!train) {
            banner.classList.add("hidden");
            banner.innerHTML = "";
            document.getElementById("summaryTrainName").textContent = "None selected";
            document.getElementById("summaryRoute").textContent = "—";
            document.getElementById("summarySeats").textContent = "—";
            return;
        }

        banner.classList.remove("hidden");
        const count = state.passengers.length;
        const insufficient = count > train.availableSeats;
        banner.innerHTML = `
            <div>
                <strong>#${train.trainNumber} ${escapeHtml(train.trainName)}</strong>
                <span> &bull; ${escapeHtml(train.source)} &rarr; ${escapeHtml(train.destination)}</span>
                ${insufficient ? `<div style="color:var(--red-700); font-weight:600; margin-top:4px;">Insufficient seats: requested ${count}, only ${train.availableSeats} available.</div>` : ""}
            </div>
            <div>
                <span class="badge ${getSeatBadge(train).cls}">${train.availableSeats} / ${train.totalSeats} seats available</span>
            </div>
        `;

        document.getElementById("summaryTrainName").textContent = `#${train.trainNumber} ${train.trainName}`;
        document.getElementById("summaryRoute").textContent = `${train.source} → ${train.destination}`;
        document.getElementById("summarySeats").textContent = `${train.availableSeats} / ${train.totalSeats}`;
    }

    function renderPassengerRows() {
        const listEl = document.getElementById("passengersList");
        listEl.innerHTML = state.passengers.map((p, idx) => `
            <div class="passenger-item" data-p-index="${idx}">
                <div class="passenger-item-head">
                    <strong>Passenger ${idx + 1}</strong>
                    ${state.passengers.length > 1
                        ? `<button type="button" class="btn-remove-passenger" data-remove-p="${idx}">Remove</button>`
                        : ""}
                </div>
                <div class="passenger-fields-grid">
                    <div class="form-group">
                        <label for="p-name-${idx}">Full Name <span class="req">*</span></label>
                        <input type="text" id="p-name-${idx}" data-field="name" data-idx="${idx}" value="${escapeHtml(p.name)}" placeholder="e.g. Aarav Sharma" required>
                        <span class="field-error" id="err-name-${idx}"></span>
                    </div>
                    <div class="form-group">
                        <label for="p-age-${idx}">Age <span class="req">*</span></label>
                        <input type="number" id="p-age-${idx}" data-field="age" data-idx="${idx}" value="${escapeHtml(p.age)}" min="1" placeholder="Age" required>
                        <span class="field-error" id="err-age-${idx}"></span>
                    </div>
                    <div class="form-group">
                        <label for="p-gender-${idx}">Gender <span class="req">*</span></label>
                        <select id="p-gender-${idx}" data-field="gender" data-idx="${idx}">
                            <option value="Male" ${p.gender === "Male" ? "selected" : ""}>Male</option>
                            <option value="Female" ${p.gender === "Female" ? "selected" : ""}>Female</option>
                            <option value="Other" ${p.gender === "Other" ? "selected" : ""}>Other</option>
                        </select>
                        <span class="field-error" id="err-gender-${idx}"></span>
                    </div>
                    <div class="form-group">
                        <label for="p-phone-${idx}">10-Digit Phone <span class="req">*</span></label>
                        <input type="tel" id="p-phone-${idx}" data-field="phoneNumber" data-idx="${idx}" value="${escapeHtml(p.phoneNumber)}" maxlength="10" inputmode="numeric" placeholder="9876543210" required>
                        <span class="field-error" id="err-phone-${idx}"></span>
                    </div>
                </div>
            </div>
        `).join("");

        listEl.querySelectorAll("[data-field]").forEach((input) => {
            const eventType = input.tagName === "SELECT" ? "change" : "input";
            input.addEventListener(eventType, (e) => {
                const idx = Number(e.target.getAttribute("data-idx"));
                const field = e.target.getAttribute("data-field");
                state.passengers[idx][field] = e.target.value;
                validateSingleField(idx, field);
            });
        });

        listEl.querySelectorAll("[data-remove-p]").forEach((btn) => {
            btn.addEventListener("click", () => {
                const idx = Number(btn.getAttribute("data-remove-p"));
                state.passengers.splice(idx, 1);
                renderPassengerRows();
                updateSelectedTrainBanner();
                updateFarePreview();
            });
        });

        document.getElementById("summaryPassengerCount").textContent = String(state.passengers.length);
    }

    function validateSingleField(idx, field) {
        const p = state.passengers[idx];
        if (field === "name") {
            const valid = p.name && p.name.trim().length > 0;
            setFieldError(`p-name-${idx}`, `err-name-${idx}`, valid ? "" : "Name is required.");
            return valid;
        }
        if (field === "age") {
            const ageNum = Number(p.age);
            const valid = Number.isInteger(ageNum) && ageNum >= 1;
            setFieldError(`p-age-${idx}`, `err-age-${idx}`, valid ? "" : "Age must be at least 1.");
            return valid;
        }
        if (field === "gender") {
            const valid = ["Male", "Female", "Other"].includes(p.gender);
            setFieldError(`p-gender-${idx}`, `err-gender-${idx}`, valid ? "" : "Select Male, Female or Other.");
            return valid;
        }
        if (field === "phoneNumber") {
            const valid = /^\d{10}$/.test((p.phoneNumber || "").trim());
            setFieldError(`p-phone-${idx}`, `err-phone-${idx}`, valid ? "" : "Enter exactly 10 digits.");
            return valid;
        }
        return true;
    }

    function setFieldError(inputId, errorId, message) {
        const input = document.getElementById(inputId);
        const err = document.getElementById(errorId);
        if (input) input.classList.toggle("input-error", Boolean(message));
        if (err) err.textContent = message;
    }

    async function updateFarePreview() {
        const trainNumber = document.getElementById("bookTrainSelect").value;
        const count = state.passengers.length;
        document.getElementById("summaryPassengerCount").textContent = String(count);

        if (!trainNumber) {
            document.getElementById("summaryFarePerSeat").textContent = "Rs. 0.00";
            document.getElementById("summaryTotalFare").textContent = "Rs. 0.00";
            return;
        }

        try {
            const params = new URLSearchParams({ trainNumber, passengers: String(count) });
            const fareData = await apiRequest(`/fare?${params.toString()}`);
            document.getElementById("summaryFarePerSeat").textContent = formatCurrency(fareData.farePerSeat);
            document.getElementById("summaryTotalFare").textContent = formatCurrency(fareData.totalFare);
        } catch (err) {
            document.getElementById("summaryTotalFare").textContent = "—";
        }
    }

    async function handleBookingSubmit(e) {
        e.preventDefault();
        const errorBanner = document.getElementById("bookingFormError");
        errorBanner.classList.add("hidden");

        const trainNumberStr = document.getElementById("bookTrainSelect").value;
        if (!trainNumberStr) {
            showBookingError("Please select a train before confirming your booking.");
            return;
        }

        let allValid = true;
        state.passengers.forEach((_, idx) => {
            if (!validateSingleField(idx, "name")) allValid = false;
            if (!validateSingleField(idx, "age")) allValid = false;
            if (!validateSingleField(idx, "gender")) allValid = false;
            if (!validateSingleField(idx, "phoneNumber")) allValid = false;
        });

        if (!allValid) {
            showBookingError("Please correct the highlighted passenger details before submitting.");
            return;
        }

        const btn = document.getElementById("confirmBookingBtn");
        btn.disabled = true;
        btn.textContent = "Processing Booking...";

        try {
            const payload = {
                trainNumber: Number(trainNumberStr),
                passengers: state.passengers.map((p) => ({
                    name: p.name.trim(),
                    age: Number(p.age),
                    gender: p.gender,
                    phoneNumber: p.phoneNumber.trim()
                }))
            };

            const booking = await apiRequest("/bookings", {
                method: "POST",
                body: JSON.stringify(payload)
            });

            showToast(`Booking confirmed! PNR: ${booking.pnrNumber}`, "success");
            await refreshTrains();
            await refreshHomeMetrics();
            renderBookingConfirmation(booking);
        } catch (err) {
            showBookingError(err.message);
        } finally {
            btn.disabled = false;
            btn.textContent = "Confirm & Book Ticket";
        }
    }

    function showBookingError(message) {
        const errorBanner = document.getElementById("bookingFormError");
        errorBanner.textContent = message;
        errorBanner.classList.remove("hidden");
    }

    function renderBookingConfirmation(booking) {
        const workspace = document.getElementById("bookingWorkspace");
        const panel = document.getElementById("bookingConfirmationPanel");
        const journeyDate = document.getElementById("bookJourneyDate").value || state.selectedJourneyDate;

        workspace.classList.add("hidden");
        panel.classList.remove("hidden");
        panel.innerHTML = `
            <div class="card confirmation-card">
                <div class="confirmation-header">
                    <div>
                        <span class="badge badge-confirmed" style="margin-bottom:8px;">BOOKING CONFIRMED</span>
                        <h2 style="color:var(--navy-900);">Your Ticket is Confirmed!</h2>
                        <p class="muted-text">Generated by Core Java BookingManager &amp; FareCalculator</p>
                    </div>
                    <div class="pnr-highlight-box">
                        <div>
                            <span>PNR Number</span>
                            <strong>${booking.pnrNumber}</strong>
                        </div>
                        <button type="button" class="btn btn-sm btn-primary" id="copyPnrBtn">Copy PNR</button>
                    </div>
                </div>

                ${renderBookingDetailBody(booking, journeyDate)}

                <div class="header-actions" style="margin-top:22px;">
                    <button type="button" class="btn btn-primary" id="bookAnotherBtn">Book Another Ticket</button>
                    <button type="button" class="btn btn-secondary" id="viewCreatedPnrBtn">View in PNR Status</button>
                    <button type="button" class="btn btn-outline" id="goMyBookingsBtn">Go to My Bookings</button>
                </div>
            </div>
        `;

        document.getElementById("copyPnrBtn").addEventListener("click", () => {
            navigator.clipboard?.writeText(String(booking.pnrNumber));
            showToast(`PNR ${booking.pnrNumber} copied to clipboard.`, "success");
        });

        document.getElementById("bookAnotherBtn").addEventListener("click", () => {
            state.passengers = [createEmptyPassenger()];
            renderPassengerRows();
            panel.classList.add("hidden");
            workspace.classList.remove("hidden");
            populateTrainSelect();
            updateFarePreview();
        });

        document.getElementById("viewCreatedPnrBtn").addEventListener("click", () => {
            document.getElementById("pnrInput").value = String(booking.pnrNumber);
            navigateTo("pnr");
            lookupPnrStatus(String(booking.pnrNumber));
        });

        document.getElementById("goMyBookingsBtn").addEventListener("click", () => navigateTo("bookings"));
    }

    // =======================================================
    // F. PNR Status
    // =======================================================
    function bindPnrEvents() {
        document.getElementById("pnrSearchForm").addEventListener("submit", async (e) => {
            e.preventDefault();
            const pnr = document.getElementById("pnrInput").value.trim();
            await lookupPnrStatus(pnr);
        });
    }

    async function lookupPnrStatus(pnr) {
        const container = document.getElementById("pnrResultContainer");
        container.innerHTML = `<div class="card empty-state"><p>Searching PNR ${escapeHtml(pnr)}...</p></div>`;

        try {
            const booking = await apiRequest(`/bookings/${encodeURIComponent(pnr)}`);
            const isConfirmed = booking.bookingStatus === "CONFIRMED";

            container.innerHTML = `
                <div class="card details-hero-card">
                    <div class="details-top">
                        <div>
                            <span class="eyebrow">PNR Lookup Result</span>
                            <h2 style="color:var(--navy-900);">PNR #${booking.pnrNumber}</h2>
                        </div>
                        <span class="badge ${isConfirmed ? "badge-confirmed" : "badge-cancelled"}" style="font-size:0.9rem; padding:7px 14px;">
                            ${escapeHtml(booking.bookingStatus)}
                        </span>
                    </div>

                    ${renderBookingDetailBody(booking)}

                    ${isConfirmed ? `
                        <div class="header-actions" style="margin-top:20px;">
                            <button type="button" class="btn btn-danger" id="pnrToCancelBtn">Cancel This Ticket</button>
                        </div>
                    ` : ""}
                </div>
            `;

            const cancelBtn = document.getElementById("pnrToCancelBtn");
            if (cancelBtn) {
                cancelBtn.addEventListener("click", () => {
                    document.getElementById("cancelPnrInput").value = String(booking.pnrNumber);
                    navigateTo("cancel");
                    lookupCancelBooking(String(booking.pnrNumber));
                });
            }
        } catch (err) {
            container.innerHTML = `
                <div class="alert alert-error">${escapeHtml(err.message)}</div>
            `;
        }
    }

    // =======================================================
    // E. Cancel Ticket
    // =======================================================
    function bindCancelEvents() {
        document.getElementById("cancelLookupForm").addEventListener("submit", async (e) => {
            e.preventDefault();
            const pnr = document.getElementById("cancelPnrInput").value.trim();
            await lookupCancelBooking(pnr);
        });

        document.getElementById("closeCancelModalBtn").addEventListener("click", closeCancelModal);
        document.getElementById("abortCancelModalBtn").addEventListener("click", closeCancelModal);
        document.getElementById("confirmCancelModalBtn").addEventListener("click", executeTicketCancellation);
    }

    async function lookupCancelBooking(pnr) {
        const container = document.getElementById("cancelResultContainer");
        container.innerHTML = `<div class="card empty-state"><p>Loading booking #${escapeHtml(pnr)}...</p></div>`;

        try {
            const booking = await apiRequest(`/bookings/${encodeURIComponent(pnr)}`);
            state.currentCancelBooking = booking;
            renderCancelBookingCard(booking);
        } catch (err) {
            state.currentCancelBooking = null;
            container.innerHTML = `<div class="alert alert-error">${escapeHtml(err.message)}</div>`;
        }
    }

    function renderCancelBookingCard(booking, statusBannerHtml = "") {
        const container = document.getElementById("cancelResultContainer");
        const isConfirmed = booking.bookingStatus === "CONFIRMED";

        container.innerHTML = `
            ${statusBannerHtml}
            <div class="card details-hero-card">
                <div class="details-top">
                    <div>
                        <span class="eyebrow">Booking Found</span>
                        <h2 style="color:var(--navy-900);">PNR #${booking.pnrNumber}</h2>
                    </div>
                    <span class="badge ${isConfirmed ? "badge-confirmed" : "badge-cancelled"}" style="font-size:0.9rem; padding:7px 14px;">
                        ${escapeHtml(booking.bookingStatus)}
                    </span>
                </div>

                ${renderBookingDetailBody(booking)}

                <div class="header-actions" style="margin-top:22px;">
                    ${isConfirmed
                        ? `<button type="button" class="btn btn-danger btn-lg" id="openCancelModalBtn">Cancel Ticket</button>`
                        : `<span class="alert alert-info" style="margin:0;">This ticket is already cancelled. Seats have been restored.</span>`
                    }
                </div>
            </div>
        `;

        const openBtn = document.getElementById("openCancelModalBtn");
        if (openBtn) {
            openBtn.addEventListener("click", () => openCancelModal(booking));
        }
    }

    function openCancelModal(booking) {
        state.pendingCancelPnr = booking.pnrNumber;
        document.getElementById("cancelModalMessage").innerHTML = `
            Are you sure you want to cancel <strong>PNR #${booking.pnrNumber}</strong> on
            <strong>${escapeHtml(booking.train.trainName)}</strong>?<br><br>
            This will release <strong>${booking.passengerCount} seat(s)</strong> back to the train's available seat pool.
        `;
        document.getElementById("cancelModal").classList.remove("hidden");
    }

    function closeCancelModal() {
        state.pendingCancelPnr = null;
        document.getElementById("cancelModal").classList.add("hidden");
    }

    async function executeTicketCancellation() {
        if (!state.pendingCancelPnr) return;
        const pnr = state.pendingCancelPnr;
        const confirmBtn = document.getElementById("confirmCancelModalBtn");
        confirmBtn.disabled = true;
        confirmBtn.textContent = "Cancelling...";

        try {
            const res = await apiRequest(`/bookings/${encodeURIComponent(pnr)}/cancel`, {
                method: "POST"
            });

            closeCancelModal();
            showToast(res.message, "success");
            await refreshTrains();
            populateTrainSelect();
            await refreshHomeMetrics();

            if (document.getElementById("section-cancel").classList.contains("active")) {
                renderCancelBookingCard(
                    res.booking,
                    `<div class="alert alert-success">${escapeHtml(res.message)}</div>`
                );
            }
            if (document.getElementById("section-bookings").classList.contains("active")) {
                await loadAllBookings();
            }
        } catch (err) {
            closeCancelModal();
            showToast(err.message, "error");
        } finally {
            confirmBtn.disabled = false;
            confirmBtn.textContent = "Yes, Cancel Ticket";
        }
    }

    // =======================================================
    // G. My Bookings
    // =======================================================
    function bindBookingsEvents() {
        document.getElementById("refreshBookingsBtn").addEventListener("click", loadAllBookings);
    }

    // =======================================================
    // H. Contact Support Form (persisted via POST /api/contact)
    // =======================================================
    function bindContactEvents() {
        const form = document.getElementById("contactForm");
        if (!form) return;

        ["contactName", "contactEmail", "contactSubject", "contactMessage"].forEach((id) => {
            const el = document.getElementById(id);
            if (el) {
                el.addEventListener("input", () => validateContactField(id));
            }
        });

        form.addEventListener("submit", handleContactSubmit);
    }

    function validateContactField(fieldId) {
        const value = (document.getElementById(fieldId)?.value || "").trim();
        if (fieldId === "contactName") {
            const ok = value.length > 0;
            setFieldError("contactName", "err-contactName", ok ? "" : "Full name is required.");
            return ok;
        }
        if (fieldId === "contactEmail") {
            const ok = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
            setFieldError("contactEmail", "err-contactEmail", ok ? "" : "Enter a valid email address.");
            return ok;
        }
        if (fieldId === "contactSubject") {
            const ok = value.length > 0;
            setFieldError("contactSubject", "err-contactSubject", ok ? "" : "Subject is required.");
            return ok;
        }
        if (fieldId === "contactMessage") {
            const ok = value.length >= 10;
            setFieldError("contactMessage", "err-contactMessage", ok ? "" : "Message must be at least 10 characters.");
            return ok;
        }
        return true;
    }

    async function handleContactSubmit(e) {
        e.preventDefault();
        const statusEl = document.getElementById("contactFormStatus");
        statusEl.className = "alert hidden";

        const fields = ["contactName", "contactEmail", "contactSubject", "contactMessage"];
        const allValid = fields.every((id) => validateContactField(id));
        if (!allValid) {
            statusEl.className = "alert alert-error";
            statusEl.textContent = "Please fix the highlighted fields before sending your message.";
            return;
        }

        const btn = document.getElementById("contactSubmitBtn");
        btn.disabled = true;
        btn.textContent = "Sending...";

        try {
            const payload = {
                name: document.getElementById("contactName").value.trim(),
                email: document.getElementById("contactEmail").value.trim(),
                subject: document.getElementById("contactSubject").value.trim(),
                message: document.getElementById("contactMessage").value.trim()
            };

            const res = await apiRequest("/contact", {
                method: "POST",
                body: JSON.stringify(payload)
            });

            statusEl.className = "alert alert-success";
            statusEl.textContent = res.message;
            showToast(res.message, "success");
            document.getElementById("contactForm").reset();
        } catch (err) {
            statusEl.className = "alert alert-error";
            statusEl.textContent = err.message;
            showToast(err.message, "error");
        } finally {
            btn.disabled = false;
            btn.textContent = "Send Message";
        }
    }

    async function loadAllBookings() {
        const container = document.getElementById("myBookingsContainer");
        container.innerHTML = `<div class="card empty-state"><p>Loading bookings...</p></div>`;

        try {
            const bookings = await apiRequest("/bookings");

            const confirmedCount = bookings.filter((b) => b.bookingStatus === "CONFIRMED").length;
            const cancelledCount = bookings.filter((b) => b.bookingStatus === "CANCELLED").length;
            document.getElementById("statTotalBookings").textContent = String(bookings.length);
            document.getElementById("statConfirmedBookings").textContent = String(confirmedCount);
            document.getElementById("statCancelledBookings").textContent = String(cancelledCount);

            if (!bookings.length) {
                container.innerHTML = `
                    <div class="card empty-state">
                        <h3>No Bookings Yet</h3>
                        <p>Once you book a train ticket, your PNR and passenger records will appear here.</p>
                        <button type="button" class="btn btn-primary" id="emptyBookBtn">Book Your First Ticket</button>
                    </div>
                `;
                document.getElementById("emptyBookBtn").addEventListener("click", () => navigateTo("book"));
                return;
            }

            container.innerHTML = `
                <div class="table-wrapper">
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>PNR Number</th>
                                <th>Train</th>
                                <th>Route</th>
                                <th>Passengers</th>
                                <th>Total Fare</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${bookings.map((b) => {
                                const isConfirmed = b.bookingStatus === "CONFIRMED";
                                const names = b.passengers.map((p) => escapeHtml(p.name)).join(", ");
                                return `
                                    <tr>
                                        <td><strong>#${b.pnrNumber}</strong></td>
                                        <td>
                                            <strong>${escapeHtml(b.train.trainName)}</strong><br>
                                            <span class="muted-text">Train #${b.train.trainNumber}</span>
                                        </td>
                                        <td>${escapeHtml(b.train.source)} &rarr; ${escapeHtml(b.train.destination)}</td>
                                        <td>
                                            <strong>${b.passengerCount}</strong> passenger(s)<br>
                                            <span class="muted-text">${names}</span>
                                        </td>
                                        <td><strong>${formatCurrency(b.totalFare)}</strong></td>
                                        <td>
                                            <span class="badge ${isConfirmed ? "badge-confirmed" : "badge-cancelled"}">
                                                ${escapeHtml(b.bookingStatus)}
                                            </span>
                                        </td>
                                        <td>
                                            <div class="table-actions">
                                                <button type="button" class="btn btn-outline btn-sm" data-pnr-view="${b.pnrNumber}">Details</button>
                                                ${isConfirmed
                                                    ? `<button type="button" class="btn btn-danger btn-sm" data-pnr-cancel="${b.pnrNumber}">Cancel</button>`
                                                    : ""
                                                }
                                            </div>
                                        </td>
                                    </tr>
                                `;
                            }).join("")}
                        </tbody>
                    </table>
                </div>
            `;

            container.querySelectorAll("[data-pnr-view]").forEach((btn) => {
                btn.addEventListener("click", () => {
                    const pnr = btn.getAttribute("data-pnr-view");
                    document.getElementById("pnrInput").value = pnr;
                    navigateTo("pnr");
                    lookupPnrStatus(pnr);
                });
            });

            container.querySelectorAll("[data-pnr-cancel]").forEach((btn) => {
                btn.addEventListener("click", () => {
                    const pnr = Number(btn.getAttribute("data-pnr-cancel"));
                    const booking = bookings.find((item) => item.pnrNumber === pnr);
                    if (booking) {
                        openCancelModal(booking);
                    }
                });
            });
        } catch (err) {
            container.innerHTML = `<div class="alert alert-error">${escapeHtml(err.message)}</div>`;
        }
    }

    // =======================================================
    // Shared Renderers & Utilities
    // =======================================================
    function renderBookingDetailBody(booking, journeyDate) {
        return `
            <div class="details-grid" style="margin-bottom:20px;">
                <div class="detail-stat-box">
                    <span>Train</span>
                    <strong>#${booking.train.trainNumber} ${escapeHtml(booking.train.trainName)}</strong>
                </div>
                <div class="detail-stat-box">
                    <span>Route</span>
                    <strong>${escapeHtml(booking.train.source)} &rarr; ${escapeHtml(booking.train.destination)}</strong>
                </div>
                <div class="detail-stat-box">
                    <span>Passengers</span>
                    <strong>${booking.passengerCount}</strong>
                </div>
                <div class="detail-stat-box">
                    <span>Total Fare</span>
                    <strong style="color:var(--orange-600);">${formatCurrency(booking.totalFare)}</strong>
                </div>
            </div>
            ${journeyDate ? `<p class="muted-text" style="margin-bottom:14px;">Selected Journey Date: <strong>${escapeHtml(journeyDate)}</strong></p>` : ""}

            <div class="table-wrapper">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Passenger ID</th>
                            <th>Name</th>
                            <th>Age</th>
                            <th>Gender</th>
                            <th>Phone Number</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${booking.passengers.map((p) => `
                            <tr>
                                <td><strong>#${p.passengerId}</strong></td>
                                <td>${escapeHtml(p.name)}</td>
                                <td>${p.age}</td>
                                <td>${escapeHtml(p.gender)}</td>
                                <td>${escapeHtml(p.phoneNumber)}</td>
                            </tr>
                        `).join("")}
                    </tbody>
                </table>
            </div>
        `;
    }

    function showToast(message, type = "info") {
        const container = document.getElementById("toastContainer");
        const toast = document.createElement("div");
        toast.className = `toast ${type}`;
        toast.textContent = message;
        container.appendChild(toast);
        setTimeout(() => {
            toast.remove();
        }, 4200);
    }

    function formatCurrency(amount) {
        const num = Number(amount);
        return Number.isFinite(num) ? `Rs. ${num.toFixed(2)}` : "Rs. 0.00";
    }

    function escapeHtml(value) {
        return String(value ?? "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }
})();
