let sidebar = document.getElementById("sidebar");

function toggleSidebar() {
    if (getSidebarState()) {
        setSidebarActive(false);
    } else {
        setSidebarActive(true);
    }
}

function getSidebarState() {
    return sidebar.classList.contains("fade-in") || (!sidebar.classList.contains("fade-out") && !sidebar.classList.contains("hidden"));
}

function setSidebarActive(setState) {
    let isState = getSidebarState();

    if (setState && !isState) {
        sidebar.classList.add("fade-in");
        sidebar.classList.remove("fade-out");
        sidebar.classList.remove("hidden");
    } else if (!setState && isState) {
        sidebar.classList.add("fade-out");
        sidebar.classList.remove("fade-in");
    }
}

function adjustSidebarVisibility() {
    if (window.innerWidth > 1500) {
        setSidebarActive(true);
    } else {
        setSidebarActive(false);
    }
}

window.onresize = function () {
    setTimeout(adjustSidebarVisibility, 400);
}

if (window.innerWidth > 1500) {
    sidebar.classList.remove("hidden");
}
