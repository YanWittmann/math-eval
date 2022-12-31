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
 * @param isInput Whether the text is input or output
 */
function appendToCodebox(codebox, text, isInput = false) {
    text = createStyleHighlightsForText(text);
    text = text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");
    let lines = text.split("\n");

    for (let i = 0; i < lines.length; i++) {
        let line = lines[i];

        let maxAttempts = 100;
        while (line.includes(":STYLE:")) {
            if (maxAttempts-- < 0) {
                console.error("Too many style tags / malformed style tag in line: " + line);
                break;
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

    let appender = codebox.getElementsByClassName("codebox-appender")[0];

    let span = document.createElement("span");
    span.classList.add("codebox-line");
    if (isInput) {
        span.classList.add("codebox-input-symbol");
    }
    span.innerHTML = lines.join("<br>");
    if (appender.childNodes.length > 0) {
        appender.innerHTML += "<br>";
    }
    appender.appendChild(span);
}

function createStyleHighlightsForText(text) {
    let keywords = ["for", "while", "if", "else", "return", "function", "true", "false", "null", "break", "continue", "import", "export", "in"];
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

let bufferedInput = {};

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
            appendToCodebox(codebox, inputText, true);
        }

        if (!event.shiftKey) {
            evaluateCodeBlock(codebox);
            inputElement.parentElement.classList.remove("multiline");
        } else {
            inputElement.parentElement.classList.add("multiline");
        }
    }
}

function evaluateCodeBlock(codebox) {
    let codeboxId = codebox.getAttribute("id");
    let codeToExecute = bufferedInput[codeboxId].join("\n");
    bufferedInput[codeboxId] = [];
    if (codeToExecute.trim() !== "") {
        let promise = evaluateCode(codeToExecute, codeboxId);
        promise.then((result) => {
            let message = "";
            if (result.print !== undefined && result.print !== null && result.print !== "") {
                let lines = result.print.split("\n").filter(v => v !== "").map(v => " " + v.trim());
                message += lines.join("\n");
            }
            if (result.result !== undefined && result.result !== null && result.result !== "") {
                if (message !== "") {
                    message += "\n";
                }
                message += "-> " + result.result;
            }
            appendToCodebox(codebox, message);

            let codeboxRect = codebox.getBoundingClientRect();
            let codeboxBottom = codeboxRect.bottom;
            let windowHeight = window.innerHeight;
            let scrollOffset = codeboxBottom - windowHeight;
            if (scrollOffset > 0) {
                window.scrollBy(0, scrollOffset);
            }
        });
    }
}

function evaluateCode(code, context) {
    return new Promise((resolve, reject) => {
        let evaluationEndpoint = "http://localhost:8000/api/guide";

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
        let evaluationEndpoint = "http://localhost:8000/api/ping";

        let xhr = new XMLHttpRequest();
        xhr.timeout = 1000;
        xhr.open("GET", evaluationEndpoint);

        xhr.setRequestHeader("Access-Control-Allow-Origin", "*");
        xhr.setRequestHeader("Content-Type", "application/json");

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

function createCodeBox(initialContent, interactive) {
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
    }

    if (initialContent !== undefined) {
        bufferedInput[codeboxId] = [initialContent];
        appendToCodebox(codeboxContainer, initialContent, true);
        if (interactive) {
            evaluateCodeBlock(codeboxContainer);
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
    evaluateCodeBlock(codebox);
}

function initializePage(interpreterIsAvailable = true) {
    let codeboxes = document.getElementsByClassName("codebox-container");

    for (let i = 0; i < codeboxes.length; i++) {
        let codebox = codeboxes[i];
        let parent = codebox.parentElement;

        if (codebox.getAttribute("initialized") === null) {
            let initialContent = codebox.getAttribute("initialContent");
            let interactive = codebox.getAttribute("interactive") !== "false";
            if (!interpreterIsAvailable) {
                interactive = false;
            }

            initialContent = initialContent == null ? "" : initialContent.replaceAll("\\n", "\n");
            parent.replaceChild(createCodeBox(initialContent, interactive), codebox);
        }
    }

    if (!interpreterIsAvailable) {
        let stickyFooter = document.createElement("div");
        stickyFooter.classList.add("sticky-footer");
        stickyFooter.classList.add("color-background-danger");
        stickyFooter.innerHTML = "Run <code>menter -gs</code> to enable interactive code blocks.";
        document.body.appendChild(stickyFooter);
    }
}

isInterpreterAvailable().then((available) => initializePage(available)).catch(() => initializePage(false));
