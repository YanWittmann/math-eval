let apiLocation = "http://localhost:26045";

let bufferedInput = {};
let lastInput = {};

let loadedCodeboxIds = [];

let isConnectedToServer = false;

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
    let lang = codebox.hasAttribute("lang") ? codebox.getAttribute("lang") : "menter";
    span.innerHTML = applyCodeFormatting(text, lang);
    if (appender.childNodes.length > 0 && appender.lastChild.classList.contains("codebox-line")) {
        appender.innerHTML += "<br>";
    }
    appender.appendChild(span);
    if (belowPadding) {
        appender.innerHTML += "<hr class='codebox-padding'>";
    }
}

function applyCodeFormatting(text, lang = "menter") {
    let originalText = text;
    text = createStyleHighlightsForText(text, lang);
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

function createStyleHighlightsForText(text, lang = "menter") {
    let isMenter = lang === "menter";
    let isXml = lang === "xml";
    let isJava = lang === "java";

    let keywords = [];
    let keywords_alt = [];
    let identifiers = [];
    let numbers = [];
    let strings = [];
    let comments = [];
    let xmlTags = [];

    if (isMenter || isJava) {
        keywords = [
            "for", "while", "if", "else", "elif", "return", "function", "true", "false", "null", "break", "continue",
            "import", "export", "in", "as", "value_function", "native_function", "reflective_function", "string",
            "number", "boolean", "array", "object", "any", "instanceof", "new", "inline"
        ];

        if (isJava) {
            keywords.push("public", "private", "protected", "class", "void", "abstract", "extends", "implements");
            keywords_alt.push("@Override", "@TypeFunction", "@TypeMetaData", "Value", "ArrayList", "List", "Arrays",
                "String", "CustomType")
        }

        identifiers = text.match(/[a-zA-Z]+/g);
        numbers = text.match(/-?\d+(\.\d+)?/g);
        strings = text.match(/["'].*?["']/g);
        comments = text.match(/###(.|\n)*?###|#.*?(\n|$)/g);
    } else if (isXml) {
        xmlTags = text.match(/<\/?[a-zA-Z]+.*?>/g);
    }

    let ansiiEscapeCodes = [
        "\u001b[30m", "\u001b[31m", "\u001b[32m", "\u001b[33m", "\u001b[34m", "\u001b[35m", "\u001b[36m", "\u001b[37m", "\u001b[0m"
    ];
    let ansiiEscapeCodeReplacements = [
        ":STYLE:COLOR:BLACK:", ":STYLE:COLOR:RED:", ":STYLE:COLOR:GREEN:", ":STYLE:COLOR:YELLOW:", ":STYLE:COLOR:BLUE:", ":STYLE:COLOR:PURPLE:", ":STYLE:COLOR:CYAN:", ":STYLE:COLOR:WHITE:", ":STYLE:END:"
    ];
    for (let i = 0; i < ansiiEscapeCodes.length; i++) {
        text = text.split(ansiiEscapeCodes[i]).join(ansiiEscapeCodeReplacements[i]);
    }

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

    for (let i = 0; i < keywords_alt.length; i++) {
        replacements[keywords_alt[i]] = ":STYLE:COLOR:CYAN:" + keywords_alt[i] + ":STYLE:END:";
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

    if (xmlTags !== null) {
        for (let i = 0; i < xmlTags.length; i++) {
            replacements[xmlTags[i]] = ":STYLE:COLOR:BLUE:" + xmlTags[i] + ":STYLE:END:";
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

function evaluateCodeBlock(codebox, initialInput, originalCodeboxId = null, initialCodebox = null) {
    let codeboxId = codebox.getAttribute("id");
    let codeToExecute = bufferedInput[codeboxId].join("\n").replaceAll(":NEWLINE:", "\n");
    bufferedInput[codeboxId] = [];

    if (codeToExecute.trim() !== "") {
        let statementSplit = initialInput ? codeToExecute.split(";;;") : [codeToExecute];

        function evaluateAndApplyCodeBlock(i) {
            evaluateCode(statementSplit[i], codeboxId).then((result) => {
                if (initialInput) {
                    let statementLines = statementSplit[i].split("\n");
                    let multiline = statementLines.length > 1;
                    for (let j = 0; j < statementLines.length; j++) {
                        let statementLine = statementLines[j];
                        appendToCodebox(codebox, statementLine, multiline ? 2 : 1, false);
                    }
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
                    if (originalCodeboxId !== null) {
                        loadedCodeboxIds.push(originalCodeboxId);
                    }
                    if (initialCodebox !== null) {
                        initialCodebox.parentElement.replaceChild(codebox, initialCodebox);
                    }
                    setLoadingStateOnCodeBox([codebox, initialCodebox], false);
                }
            }).catch((error) => {
                appendToCodebox(codebox, "It seems that the connection to the Menter server has been lost.", 0, true);
                setLoadingStateOnCodeBox([codebox, initialCodebox], false);
            });
        }

        setLoadingStateOnCodeBox([codebox, initialCodebox], true);
        evaluateAndApplyCodeBlock(0);
    }
}

let codeBoxLoadingState = {};

function setLoadingStateOnCodeBox(codebox, loading) {
    for (let i = 0; i < codebox.length; i++) {
        if (codebox[i] !== null && codebox[i].classList !== undefined) {
            codeBoxLoadingState[codebox[i].getAttribute("id")] = loading;
            if (loading) {
                setTimeout(() => {
                    let isInconsistentState = codeBoxLoadingState[codebox[i].getAttribute("id")] !== undefined && codeBoxLoadingState[codebox[i].getAttribute("id")] !== loading;
                    if (!isInconsistentState) {
                        codebox[i].classList.add("loading");
                    }
                }, 600);
            } else {
                codebox[i].classList.remove("loading");
            }
        }
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

function sendDestroyCodeBoxToServer(codeboxId) {
    let evaluationEndpoint = apiLocation + "/api/guide";

    let xhr = new XMLHttpRequest();
    xhr.open("POST", evaluationEndpoint);

    xhr.setRequestHeader("Access-Control-Allow-Origin", "*");
    xhr.setRequestHeader("Content-Type", "application/json");

    let content = JSON.stringify({
        context: codeboxId,
        destroy: true
    });
    xhr.setRequestHeader("Content-Length", content.length + "");
    xhr.send(content);

    xhr.onload = function () {
        let code = xhr.status;
        if (code === 200 && !xhr.responseText.includes("error")) {
            console.log("Successfully destroyed codebox: " + codeboxId);
        } else {
            console.error("Error while destroying codebox: " + codeboxId);
        }
    };

    xhr.onerror = function () {
        console.error("Error while destroying codebox: " + codeboxId);
    };
}

function sendDestroyAllCodeBoxesToServer() {
    if (!isConnectedToServer) return;

    let codeboxes = document.getElementsByClassName("codebox-container");
    for (let i = 0; i < codeboxes.length; i++) {
        let codebox = codeboxes[i];
        if (codebox.getAttribute("initialized") === "true") {
            sendDestroyCodeBoxToServer(codebox.getAttribute("id"));
        }
    }
}

function createCodeBox(initialContent, interactive, originalCodeboxId, initialCodebox, setInitialized = true) {
    let codeboxId = "codebox-" + Math.floor(Math.random() * 1000000);

    let codeboxContainer = document.createElement("div");

    for (let i = 0; i < initialCodebox.attributes.length; i++) {
        let attribute = initialCodebox.attributes[i];
        if (attribute.name !== "id" && attribute.name !== "class" && attribute.name !== "style" && attribute.name !== "initialized") {
            codeboxContainer.setAttribute(attribute.name, attribute.value);
        }
    }
    codeboxContainer.classList.add("codebox-container");
    codeboxContainer.setAttribute("initialized", "" + setInitialized);
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

        let resetCodeboxButton = document.createElement("button");
        resetCodeboxButton.classList.add("codebox-reset-button");
        resetCodeboxButton.classList.add("fade-in");
        resetCodeboxButton.setAttribute("title", "Reset codebox");
        resetCodeboxButton.onclick = function () {
            resetCodebox(codeboxContainer);
        }
        codeboxContainer.appendChild(resetCodeboxButton);
    }

    if (initialContent !== undefined && initialContent !== null && setInitialized) {
        bufferedInput[codeboxId] = [initialContent];
        if (interactive) {
            evaluateCodeBlock(codeboxContainer, true, originalCodeboxId, initialCodebox);
        }
    }

    return codeboxContainer;
}

function resetCodebox(codeboxContainer) {
    codeboxContainer.setAttribute("id", "codebox-" + Math.floor(Math.random() * 1000000));
    let codeboxId = codeboxContainer.getAttribute("id");
    let codeboxAppender = codeboxContainer.getElementsByClassName("codebox-appender")[0];
    codeboxAppender.innerHTML = "";
    let initialContent = codeboxContainer.getAttribute("initialContent");
    initialContent = initialContent == null ? "" : initialContent.replaceAll("\\\\", "\\").replaceAll("<br>", "\n");
    bufferedInput[codeboxId] = initialContent === "" ? [] : [initialContent];
    evaluateCodeBlock(codeboxContainer, true, codeboxId, codeboxContainer);
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
    isConnectedToServer = true;

    let codeboxes = document.getElementsByClassName("codebox-container");

    for (let i = 0; i < codeboxes.length; i++) {
        let initialCodebox = codeboxes[i];

        if (initialCodebox.getAttribute("initialized") === null || initialCodebox.getAttribute("initialized") === "false") {
            let initialContent = initialCodebox.getAttribute("initialContent");
            initialContent = initialContent == null ? "" : initialContent.replaceAll("\\\\", "\\").replaceAll("<br>", "\n");

            let uninteractiveResult = initialCodebox.getAttribute("result");
            let uninteractiveResultSplit = uninteractiveResult === null || uninteractiveResult === undefined || uninteractiveResult === "" ? [] : uninteractiveResult.split(";;;");

            let interactive = initialCodebox.getAttribute("interactive") !== "false";
            if (!interpreterIsAvailable) {
                interactive = false;
            }

            let statementSplit = initialContent.split(";;;");

            let originalCodeboxId = isNotEmpty(initialCodebox.getAttribute("codebox-id")) ? "codebox-" + initialCodebox.getAttribute("codebox-id") : null;
            let afterCodeboxWithId = isNotEmpty(initialCodebox.getAttribute("after")) ? "codebox-" + initialCodebox.getAttribute("after") : null;

            let interval = setInterval(() => {
                if (!interactive || isEmpty(afterCodeboxWithId) || loadedCodeboxIds.includes(afterCodeboxWithId)) {
                    clearInterval(interval);

                    let newCodebox = createCodeBox(initialContent, interactive, originalCodeboxId, initialCodebox);
                    if (!interactive) {
                        appendCodeboxOutput(newCodebox, statementSplit, uninteractiveResultSplit);
                    }
                }
            }, 500);
        }
    }

    if (!interpreterIsAvailable && codeboxes.length > 0) {
        addWarningText("Learn how to enable <a href='execute_code.html'>interactive code blocks</a>", 2);
        setIntervalX(() => {
            isInterpreterAvailable().then(nowAvailable => {
                if (nowAvailable) window.location.reload();
            }).catch(() => {
                console.error("Failed to check interpreter availability");
            });
        }, 8 * 1000, 3);
    }
}

function preLoadCodeBoxesOnPage() {
    let codeboxes = document.getElementsByClassName("codebox-container");

    for (let i = 0; i < codeboxes.length; i++) {
        let initialCodebox = codeboxes[i];
        let parent = initialCodebox.parentElement;

        if (initialCodebox.getAttribute("initialized") === null || initialCodebox.getAttribute("initialized") === "false") {
            let initialContent = initialCodebox.getAttribute("initialContent");
            initialContent = initialContent == null ? "" : initialContent.replaceAll("\\\\", "\\").replaceAll("<br>", "\n");

            let uninteractiveResult = initialCodebox.getAttribute("result");
            let uninteractiveResultSplit = uninteractiveResult === null || uninteractiveResult === undefined || uninteractiveResult === "" ? [] : uninteractiveResult.split(";;;");

            let statementSplit = initialContent.split(";;;");

            let tempCodebox = createCodeBox(initialContent, false, null, initialCodebox, false);
            appendCodeboxOutput(tempCodebox, statementSplit, uninteractiveResultSplit);
            parent.replaceChild(tempCodebox, initialCodebox);
        }
    }
}

function appendCodeboxOutput(codebox, statements, results) {
    let isNotMenter = codebox.hasAttribute("lang") && codebox.getAttribute("lang") !== "menter";

    for (let j = 0; j < statements.length; j++) {
        let lines = statements[j].split(":NEWLINE:");
        let isMultiLine = lines.length > 1;
        for (let k = 0; k < lines.length; k++) {
            let inputType = isNotMenter ? 0 : (isMultiLine ? 2 : 1);
            let belowPadding = k === lines.length - 1 && results[j] === undefined && !isNotMenter;
            appendToCodebox(codebox, lines[k], inputType, belowPadding);
        }

        if (results[j] !== undefined) {
            appendToCodebox(codebox, "-> " + results[j], 0, true);
        }
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

function initializeCodeBoxesOnPage() {
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

    preLoadCodeBoxesOnPage();
    isInterpreterAvailable()
        .then((available) => initializePage(available))
        .catch(() => initializePage(false));

    applyFormattingToAllCodeTags();
}

initializeCodeBoxesOnPage();
