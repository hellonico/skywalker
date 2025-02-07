# Work

- https://huggingface.co/mradermacher/Tanuki-7B-v0.1-GGUF/tree/main
- 
# Skywalker Version 2

Flow in this second version.

1. prep phase
    1. parse the pdf to md via llama parse (or our custom-text reader) to markdown.
    1. the lucene index will be created here via the already pre-processed pdf->md files.
1. parse the question into keywords
2. search for relevant documents based on the keywords
4. depending on the augemented strategy, fetches:
    1. :full the full document
    2. :sentences only relevant sentences
    3. :parts parts of the documents. -> close to full document but removes section that are probably not interesting

5. depending on the stategy uses more context space
6. get the answer using augmented context
7. output to csv file

This uses two ollama-fn.

## The keyworder

> gets some text and retrieve the most important keywords of that text

```clojure
{
:url    "http://localhost:11431"
:model  "llama3.1"
:format
{
 :type     "array"
 :minItems 4
 :maxItems 8
 :items    {:type       "object"
            :required   [:keyword :relevance]
            :properties {:keyword {:type "string" :minLength 2} :relevance {:type "integer"}}}}
:system
"In Japanese, Find all the main keywords in the each prompt. relevance is how important it is in the sentence betweem 1 and 10"
:stream false}
```

## The answerer
> get an augmented context (knowledge), and answer the question

```clojure
{
 :url     "http://localhost:11432"
 :model   "llama3.1"
 :format  {:type "string" :maxLength 100}
 :pre     "以下の文が与えられています: %s. 正確に質問 %s に短く、コメントなし。"
 :system  "与えられテキストだけで、日本語でできるだけ短い文書で答えよ。一行、短く。嘘つかない。返事わからない場合は「わかりません」と返してください。"
 :options {:num_ctx 15200 :temperature 0.8}
 :stream  false}
```

# SkyWalker 01

export OLLAMA_URL=http://localhost:11434
export OLLAMA_URL=http://localhost:11432
export OLLAMA_MODEL=mistral

cd pyjama-skywalker
clj -M -m pyjama.skywalker <folder> <skip_question_mappings> (true or false)
java -jar target/skywalker-1.0.no.git-jdk21.0.5.jar <folder> <skip_question_mappings>

clj -M -m pyjama.skywalker skywalker03 true