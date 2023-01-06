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

let isWarningTextHovered = false;

function addWarningText(text) {
    let stickyFooter = document.getElementById("sticky-footer");
    if (!stickyFooter) {
        stickyFooter = document.createElement("div");
        stickyFooter.setAttribute("id", "sticky-footer");
        stickyFooter.classList.add("hidden");
        stickyFooter.classList.add("sticky-footer");
        stickyFooter.classList.add("color-background-danger");
        document.body.appendChild(stickyFooter);
    }

    if (stickyFooter.innerHTML.length > 0) {
        stickyFooter.innerHTML += "<br>";
    }
    stickyFooter.innerHTML += text;

    let stickyFooterIcon = document.getElementById("sticky-footer-icon");
    if (!stickyFooterIcon) {
        stickyFooterIcon = document.createElement("div");
        stickyFooterIcon.id = "sticky-footer-icon";
        stickyFooterIcon.classList.add("sticky-footer-icon");
        stickyFooterIcon.innerText = "!";
        stickyFooterIcon.onmouseenter = function () {
            if (!isWarningTextHovered) {
                stickyFooter.classList.remove("hidden");
                stickyFooterIcon.classList.add("hidden");
                isWarningTextHovered = true;

                let mouseMoveListener = function (e) {
                    if (e.clientY < window.innerHeight - 200 && !stickyFooter.contains(e.target)) {
                        stickyFooter.classList.add("hidden");
                        stickyFooterIcon.classList.remove("hidden");
                        isWarningTextHovered = false;
                        document.removeEventListener("mousemove", mouseMoveListener);
                    }
                }
                document.addEventListener("mousemove", mouseMoveListener);
            }
        }
        document.body.appendChild(stickyFooterIcon);
    }
}