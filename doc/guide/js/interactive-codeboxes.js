let apiLocation = "http://localhost:26045";

let bufferedInput = {};
let lastInput = {};

let loadedCodeboxIds = [];

/**
 * Appends the text provided to the end of the codebox.
 * Supports:
 *  - multiple lines separated by newlines
 *  - colors via `:STYLE:COLOR:RED:` and ending of colors with `:STYLE:END:`
 *  - other styles like italic or bold via `:STYLE:ITALIC:` and ending with `:STYLE:END:`
 *
 *  these `:STYLE:` tags can be nested and stretch over multiple lines. An example would be:
 *  ```
 *  This is :STYLE:ITALIC::STYLE:BOLD:italic and
 *  bold:STYLE:END: and this is only italic:STYLE:END:
 *  ```
 *  which would result in:
 *  This is <span class="italic"><span class="bold">italic and<br>bold</span> and this is only italic</span>
 *
 * @param codebox The codebox to append to
 * @param text The text to append
 * @param inputType Whether the text is input or output
 * @param belowPadding Whether to add padding below the text
 */
function appendToCodebox(codebox, text, inputType, belowPadding) {
    let appender = codebox.getElementsByClassName("codebox-appender")[0];

    let span = document.createElement("span");
    span.classList.add("codebox-line");
    if (inputType === 1) {
        span.classList.add("codebox-input-symbol");
    } else if (inputType === 2) {
        span.classList.add("codebox-input-symbol");
        span.classList.add("multiline");
    }
    span.innerHTML = applyCodeFormatting(text);
    if (appender.childNodes.length > 0 && appender.lastChild.classList.contains("codebox-line")) {
        appender.innerHTML += "<br>";
    }
    appender.appendChild(span);
    if (belowPadding) {
        appender.innerHTML += "<hr class='codebox-padding'>";
    }
}

function applyCodeFormatting(text) {
    let originalText = text;
    text = createStyleHighlightsForText(text);
    text = text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");
    let lines = text.split("\n");

    for (let i = 0; i < lines.length; i++) {
        let line = lines[i];

        let maxAttempts = 1000;
        while (line.includes(":STYLE:")) {
            if (maxAttempts-- < 0) {
                console.error("Too many style tags or malformed style tag in line: " + line);
                return originalText.split("\n").join("<br>");
            }

            let styleStart = line.indexOf(":STYLE:");
            let styleEnd = styleStart + 6;
            let expectedArgumentCount = 0;
            let buffer = "";
            let args = [];

            while (styleEnd < line.length) {
                styleEnd += 1;
                let char = line.charAt(styleEnd);
                if (char === ":") {
                    args.push(buffer);
                    buffer = "";
                    if (args.length === 1) {
                        if (args[0] === "END") {
                            expectedArgumentCount = 1;
                        } else if (args[0] === "ITALIC" || args[0] === "BOLD" || args[0] === "UNDERLINE" || args[0] === "STRIKETHROUGH") {
                            expectedArgumentCount = 1;
                        } else if (args[0] === "COLOR") {
                            expectedArgumentCount = 2;
                        }
                    }

                    if (args.length === expectedArgumentCount) {
                        break;
                    }
                } else {
                    buffer += char;
                }
            }

            if (args.length === expectedArgumentCount) {
                let style = args[0];
                let styleClass = "";
                if (style === "ITALIC") {
                    styleClass = "italic";
                } else if (style === "BOLD") {
                    styleClass = "bold";
                } else if (style === "UNDERLINE") {
                    styleClass = "underline";
                } else if (style === "STRIKETHROUGH") {
                    styleClass = "strikethrough";
                } else if (style === "COLOR") {
                    styleClass = "color-text-" + args[1].toLowerCase();
                }

                let before = line.substring(0, styleStart);
                let after = line.substring(styleEnd + 1);

                if (styleClass !== "") {
                    line = before + "<span class=\"" + styleClass + "\">" + after;
                } else if (style === "END") {
                    line = before + "</span>" + after;
                }
            }
        }

        lines[i] = line;
    }

    return lines.join("<br>");
}

function createStyleHighlightsForText(text) {
    let keywords = ["for", "while", "if", "else", "return", "function", "true", "false", "null", "break", "continue", "import", "export", "in", "as"];
    let identifiers = text.match(/[a-zA-Z]+/g);
    let numbers = text.match(/-?\d+(\.\d+)?/g);
    let strings = text.match(/["'].*?["']/g);
    let comments = text.match(/###(.|\n)*?###|#.*?(\n|$)/g);

    let replacements = {};

    if (identifiers !== null) {
        identifiers = identifiers.sort((a, b) => b.length - a.length).filter(v => !keywords.includes(v));
        for (let i = 0; i < identifiers.length; i++) {
            if (!keywords.includes(identifiers[i])) {
                replacements[identifiers[i]] = ":STYLE:COLOR:PURPLE:" + identifiers[i] + ":STYLE:END:";
            }
        }
    }

    for (let i = 0; i < keywords.length; i++) {
        replacements[keywords[i]] = ":STYLE:COLOR:PINK:" + keywords[i] + ":STYLE:END:";
    }

    if (numbers !== null) {
        for (let i = 0; i < numbers.length; i++) {
            replacements[numbers[i]] = ":STYLE:COLOR:BLUE:" + numbers[i] + ":STYLE:END:";
        }
    }

    if (strings !== null) {
        for (let i = 0; i < strings.length; i++) {
            replacements[strings[i]] = ":STYLE:COLOR:GREEN:" + strings[i] + ":STYLE:END:";
        }
    }

    if (comments !== null) {
        for (let i = 0; i < comments.length; i++) {
            replacements[comments[i]] = ":STYLE:COLOR:GRAY:" + comments[i] + ":STYLE:END:";
        }
    }

    function lookaheadEquals(str, i, text) {
        let lookahead = "";
        for (let j = 0; j < str.length; j++) {
            lookahead += text.charAt(i + j + 1);
        }
        return lookahead === str;
    }

    let replacementOrder = Object.keys(replacements).sort((a, b) => b.length - a.length);
    for (let key in replacementOrder) {
        let replacement = replacements[replacementOrder[key]];

        let canReplace = true;
        let buffer = "";
        let maxIterations = text.length * 5;
        for (let i = 0; i < text.length; i++) {
            if (maxIterations-- < 0) {
                console.error("Too many iterations on text highlighter");
                break;
            }

            let char = text.charAt(i);
            buffer += char;
            if (buffer.endsWith(":")) {
                if (lookaheadEquals("STYLE:", i, text)) {
                    canReplace = false;
                }
            }
            if (buffer.endsWith(":STYLE:")) {
                canReplace = false;
            } else if (buffer.endsWith(":STYLE:END:")) {
                canReplace = true;
                buffer = "";
            }

            if (canReplace && buffer.endsWith(replacementOrder[key])) {
                text = text.substring(0, i - replacementOrder[key].length + 1) + replacement + text.substring(i + 1);
                buffer = "";
                i += replacement.length - replacementOrder[key].length;
            }
        }
    }

    return text;
}

function codeBlockInteracted(inputElement, event) {
    if (event.key === "Enter") {
        event.preventDefault();
        let inputText = inputElement.value;
        inputElement.value = "";
        let codebox = inputElement.parentElement.parentElement;
        let codeboxId = codebox.getAttribute("id");

        if (inputText.trim() !== "") {
            if (bufferedInput[codeboxId] === undefined) {
                bufferedInput[codeboxId] = [];
            }
            bufferedInput[codeboxId].push(inputText);
            appendToCodebox(codebox, inputText, event.shiftKey ? 2 : 1, false);
        }

        lastInput[codeboxId] = inputText;

        if (!event.shiftKey) {
            evaluateCodeBlock(codebox, false);
            inputElement.parentElement.classList.remove("multiline");
        } else {
            inputElement.parentElement.classList.add("multiline");
        }

    } else if (event.key === "ArrowUp") {
        let codeboxId = inputElement.parentElement.parentElement.getAttribute("id");
        if (lastInput[codeboxId] !== undefined && lastInput[codeboxId].length > 0) {
            inputElement.value = lastInput[codeboxId];
            event.preventDefault();
        }
    }
}

function evaluateCodeBlock(codebox, initialInput, originalCodeboxId) {
    let codeboxId = codebox.getAttribute("id");
    let codeToExecute = bufferedInput[codeboxId].join("\n").replaceAll(":NEWLINE:", "\n");
    bufferedInput[codeboxId] = [];

    if (codeToExecute.trim() !== "") {
        let statementSplit = initialInput ? codeToExecute.split(";;;") : [codeToExecute];

        function evaluateAndApplyCodeBlock(i) {
            evaluateCode(statementSplit[i], codeboxId).then((result) => {
                if (initialInput) {
                    appendToCodebox(codebox, statementSplit[i], 1, false);
                }

                let message = "";
                if (isNotEmpty(result.print)) {
                    result.print = result.print.replaceAll("\r", "");
                    let lines = result.print.split("\n").filter(v => v !== "").map(v => " " + v);
                    message += lines.join("\n");
                }
                if (isNotEmpty(result.result)) {
                    if (message !== "") {
                        message += "\n";
                    }
                    message += "-> " + result.result.split("\n").join("\n   ");
                }
                appendToCodebox(codebox, message, 0, true);

                if (!initialInput) {
                    let codeboxRect = codebox.getBoundingClientRect();
                    let codeboxBottom = codeboxRect.bottom;
                    let windowHeight = window.innerHeight;
                    let scrollOffset = codeboxBottom - windowHeight;
                    if (scrollOffset > 0) {
                        window.scrollBy(0, scrollOffset + 100);
                    }
                }

                if (i + 1 < statementSplit.length) {
                    evaluateAndApplyCodeBlock(i + 1);
                } else {
                    loadedCodeboxIds.push(originalCodeboxId);
                }
            });
        }

        evaluateAndApplyCodeBlock(0);
    }
}

function evaluateCode(code, context) {
    return new Promise((resolve, reject) => {
        let evaluationEndpoint = apiLocation + "/api/guide";

        let xhr = new XMLHttpRequest();
        xhr.open("POST", evaluationEndpoint);

        xhr.setRequestHeader("Access-Control-Allow-Origin", "*");
        xhr.setRequestHeader("Content-Type", "application/json");

        let content = JSON.stringify({
            code: code,
            context: context
        });
        xhr.setRequestHeader("Content-Length", content.length + "");
        xhr.send(content);

        xhr.onload = function () {
            resolve(JSON.parse(xhr.responseText));
        };

        xhr.onerror = function () {
            reject(xhr.statusText);
        };
    });
}

function isInterpreterAvailable() {
    return new Promise((resolve, reject) => {
        let evaluationEndpoint = apiLocation + "/api/ping";

        let xhr = new XMLHttpRequest();
        xhr.timeout = 1000;
        xhr.open("GET", evaluationEndpoint);

        //xhr.setRequestHeader("Access-Control-Allow-Origin", "*");
        //xhr.setRequestHeader("Content-Type", "application/json");

        xhr.send();

        xhr.onload = function () {
            let code = xhr.status;
            resolve(code === 200);
        }

        xhr.onerror = function () {
            reject(false);
        }

        xhr.ontimeout = function () {
            reject(false);
        }
    });
}

function createCodeBox(initialContent, interactive, originalCodeboxId) {
    let codeboxContainer = document.createElement("div");
    codeboxContainer.classList.add("codebox-container");
    codeboxContainer.setAttribute("initialized", "true");
    let codeboxId = "codebox-" + Math.floor(Math.random() * 1000000);
    codeboxContainer.setAttribute("id", codeboxId);

    let codeboxAppender = document.createElement("div");
    codeboxAppender.classList.add("codebox-appender");
    codeboxContainer.appendChild(codeboxAppender);

    if (interactive) {
        let codeboxInputContainer = document.createElement("span");
        codeboxInputContainer.classList.add("codebox-input-container");
        codeboxInputContainer.classList.add("codebox-input-symbol");
        codeboxContainer.appendChild(codeboxInputContainer);

        let codeboxInput = document.createElement("input");
        codeboxInput.setAttribute("placeholder", "_");
        codeboxInput.setAttribute("spellcheck", "false");
        codeboxInput.onkeyup = function (event) {
            codeBlockInteracted(this, event);
        };
        codeboxInputContainer.appendChild(codeboxInput);

        let mobileOnlySubmitButton = document.createElement("button");
        mobileOnlySubmitButton.classList.add("mobile-only");
        mobileOnlySubmitButton.classList.add("codebox-submit-button");
        mobileOnlySubmitButton.innerHTML = "Submit";
        mobileOnlySubmitButton.onclick = function () {
            evaluateCodeBlockFromSubmitButton(codeboxContainer);
        }
        codeboxInputContainer.appendChild(mobileOnlySubmitButton);

        let drag = false;
        codeboxContainer.addEventListener('mousedown', () => drag = false);
        codeboxContainer.addEventListener('mousemove', () => drag = true);
        codeboxContainer.addEventListener('mouseup', () => {
            if (!drag) codeboxInput.focus();
        });
    }

    if (initialContent !== undefined) {
        bufferedInput[codeboxId] = [initialContent];
        if (interactive) {
            evaluateCodeBlock(codeboxContainer, true, originalCodeboxId);
        }
    }

    return codeboxContainer;
}

function evaluateCodeBlockFromSubmitButton(codebox) {
    codeBlockInteracted(codebox.getElementsByClassName("codebox-input-container")[0].childNodes[0], {
        key: "Enter",
        shiftKey: false,
        preventDefault: () => {
        }
    });
    evaluateCodeBlock(codebox, false);
}

function initializePage(interpreterIsAvailable = true) {
    let codeboxes = document.getElementsByClassName("codebox-container");

    for (let i = 0; i < codeboxes.length; i++) {
        let codebox = codeboxes[i];
        let parent = codebox.parentElement;

        if (codebox.getAttribute("initialized") === null) {
            let initialContent = codebox.getAttribute("initialContent");
            initialContent = initialContent == null ? "" : initialContent.replaceAll("\\\\", "\\").replaceAll("<br>", "\n");

            let uninteractiveResult = codebox.getAttribute("result");
            uninteractiveResult = uninteractiveResult === null || uninteractiveResult === undefined || uninteractiveResult === "" ? null : uninteractiveResult;

            let interactive = codebox.getAttribute("interactive") !== "false";
            if (!interpreterIsAvailable) {
                interactive = false;
            }

            let originalCodeboxId = isNotEmpty(codebox.getAttribute("id")) ? "codebox-" + codebox.getAttribute("id") : null;
            let afterCodeboxWithId = isNotEmpty(codebox.getAttribute("after")) ? "codebox-" + codebox.getAttribute("after") : null;

            let interval = setInterval(() => {
                if (!interactive || isEmpty(afterCodeboxWithId) || loadedCodeboxIds.includes(afterCodeboxWithId)) {
                    clearInterval(interval);

                    let newCodebox = createCodeBox(initialContent, interactive, originalCodeboxId);
                    if (!interactive) {
                        let statementSplit = initialContent.split(";;;");
                        let uninteractiveSplit = uninteractiveResult !== null ? uninteractiveResult.split(";;;") : [];
                        for (let j = 0; j < statementSplit.length; j++) {
                            let lines = statementSplit[j].split(":NEWLINE:");
                            for (let k = 0; k < lines.length; k++) {
                                appendToCodebox(newCodebox, lines[k], k === lines.length - 1 ? 1 : 2, k > 0);
                            }

                            if (uninteractiveSplit[j] !== undefined) {
                                appendToCodebox(newCodebox, "-> " + uninteractiveSplit[j], 0, true);
                            }
                        }
                    }
                    parent.replaceChild(newCodebox, codebox);
                }
            }, 500);
        }
    }

    if (!interpreterIsAvailable && codeboxes.length > 0) {
        addWarningText("Learn how to enable <a href='Hints_evaluation_server.html'>interactive code blocks</a>", 2);
        setIntervalX(() => {
            isInterpreterAvailable().then(nowAvailable => {
                if (nowAvailable) window.location.reload();
            }).catch(() => {
                console.error("Failed to check interpreter availability");
            });
        }, 8 * 1000, 3);
    }
}

function setIntervalX(callback, delay, repetitions) {
    let x = 0;
    let intervalID = window.setInterval(function () {
        callback();
        if (++x === repetitions) {
            window.clearInterval(intervalID);
        }
    }, delay);
}

function applyFormattingToAllCodeTags() {
    let codeTags = document.getElementsByTagName("code");
    for (let i = 0; i < codeTags.length; i++) {
        let codeTag = codeTags[i];
        let code = codeTag.innerText;
        codeTag.innerHTML = applyCodeFormatting(code);
    }
}

function getGetParameters() {
    let queryDict = {};
    location.search.substr(1)
        .split("&")
        .forEach(function (item) {
            queryDict[item.split("=")[0]] = item.split("=")[1]
        });
    return queryDict;
}

function isNotEmpty(variable) {
    return variable !== null && variable !== undefined && variable !== "";
}

function isEmpty(variable) {
    return !isNotEmpty(variable);
}

function initializeCodeBox() {
    let params = getGetParameters();
    let loadedFromStorage = false;
    if (isNotEmpty(params["host"]) && isNotEmpty(params["port"])) {
        apiLocation = "http://" + params["host"] + ":" + params["port"];
        sessionStorage.setItem("menterApiLocation", apiLocation);
    } else if (localStorage.getItem("menterApiLocation") !== null) {
        apiLocation = localStorage.getItem("apiLocation");
        loadedFromStorage = true;
    } else if (sessionStorage.getItem("menterApiLocation") !== null) {
        apiLocation = sessionStorage.getItem("menterApiLocation");
        loadedFromStorage = true;
    }

    if (loadedFromStorage) {
        setTimeout(() => {
            addWarningText("Server: <code>" + apiLocation + "</code> - " +
                "<a href='#' onclick='localStorage.removeItem(\"menterApiLocation\"); sessionStorage.removeItem(\"menterApiLocation\"); window.location.reload();'>Reset</a>", 1);
        }, 500);
    }

    isInterpreterAvailable()
        .then((available) => initializePage(available))
        .catch(() => initializePage(false));

    applyFormattingToAllCodeTags();
}

initializeCodeBox();
