(ns erinite.lib.navigation)

; DATA STRUCTURE:
; root-page-info  {:children "set of child pages"
;                  :default "default page if root invisible, omitted if not"
;                  :view    "map of placeholder ids to view ids"}
; page-info       {:children "set of child pages"
;                  :view    "map of placeholder ids to view ids"}
; document-structure {document-name {:*         root-page-info
;                                    page-name  page-info}}
;
; {:params "map of parameter ids to parameter values"
;  :docs "stack (vector) of open documents"
;  :path "stack (vector) of open pages"  ; Stored as vec of [doc page] pairs
;  :structure document-structure }
  

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers


(defn make-nav-state
  "Create navigation structure"
  [document-structure initial-document]
  {:params            {}
   :default-document  initial-document
   :path              [[initial-document :*]]
   :structure         document-structure})


(defn close*
  "Close current document by stripping it from path"
  [{:keys [path] :as nav}]
  (let [[current-doc _] (peek path)
        reverse-path    (rseq path) ; Reverse so current doc can be removed
        prev-doc-path   (into [] (drop-while ; Remove current document
                                   #(= current-doc (first %))
                                   reverse-path))
        new-path        (vec (rseq prev-doc-path))]
    #_(println (into [] (take-while #(= current-doc (first %) reverse-path))))
    ;; Replace path with new path, but make sure it cannot be empty
    (assoc nav :path (if (empty? new-path) [[current-doc :*]] new-path))))


(defn forward*
  "Navigate page forward by updating path"
  [{:keys [path structure] :as nav} page-keyword]
  (let [[current-doc _] (peek path)
        ;; Get children of current page
        children        (->> path peek (get-in structure) :children)
        ;; Get the document that the new page is in
        doc             (namespace page-keyword)
        ;; If the page is a document name, the document name exists and the
        ;; current page has that document root as a child, then the document
        ;; name is a valid shortcut for :document-name/*
        [doc page-keyword]  (if (and (empty? doc) ; Document not specified
                                     ;; And page is a valid document name
                                     (contains? structure page-keyword)
                                     ;; And document root is a child
                                     (contains?
                                        children
                                        (keyword (name page-keyword) "*")))
                          [page-keyword "*"]  ; Then replace with this doc
                          [doc page-keyword]) ; Otherwise leave unchanged
        ;; If the page was namespaced, get the page portion
        new-page        (if doc (keyword (name page-keyword)) page-keyword)
        ;; If namespaced, get the namespace, otherwise use current doc
        new-doc         (if doc (keyword doc) current-doc)
        ;; New path entry is a pair of [document page]
        path-entry      [new-doc new-page]]
    (if (or (and (= current-doc new-doc)        ; If page is not another doc 
                 (contains? children new-page)) ; ..and page is in doc
            (contains? children page-keyword))  ; OR page is in another doc
      (update-in nav [:path] conj path-entry)   ; Then add page to path.
      nav)))  ; Otherwise page is not in doc nor in another doc, do nothing


(defn backward*
  "Navigate page backward by removing current page from end of path"
  [{:keys [path] :as nav} current-doc]
  (let [new-path      (pop path) 
        ;; If the new path is empty, set it to root page of current doc
        new-path      (if (= new-path []) [[current-doc :*]] new-path) 
        [new-doc _]   (peek new-path)]   ; Get the new document from the path
    (assoc nav :path new-path))) ; Set the new path
  

(defn fix-doc-root*
  "If the page is the doc roo and it has :default set then the page must be
   modified.
    If direction is :forward, then change the page to the :default instead.
    If direction is :backward, then move backward one more time"
  [{:keys [path structure] :as nav} direction]
  (let [[doc page] (peek path)]
    (if-let [default (and
                       (= page :*)                             ; doc root, and
                      (get-in structure [doc page :default]))] ; default is set
      (case direction
        ;; For forward, add the default to the path
        :forward  (update-in nav [:path] conj [doc default])
        ;; For backward, move backward one more time
        :backward (backward* nav doc))
      ;; Not a doc root or no default set, so leave unchanged
      nav)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Page API


(defn forward!
  "Go to a child page (may open new document)"
  [navigation page-keyword params & [dont-fix-root?]]
  (let [fix-root (if-not dont-fix-root?
                   #(fix-doc-root* % :forward)
                   identity)]
    (swap!
      (:nav-state navigation)
      (fn [nav]
        ; Navigate page forward and merge in new parameters
        (-> (forward* nav page-keyword)
            (update-in [:params] merge params)
            (fix-doc-root* :forward))))))


(defn backward!
  "Go to the parent page (may close an open document)"
  [navigation & [dont-fix-root?]]
  (swap!
    (:nav-state navigation)
    (fn [{:keys [path] :as nav}]
      (let [[current-doc page] (peek path)
            fix-root  (if-not dont-fix-root?
                        #(fix-doc-root* % :backward)        
                        identity)
            new-nav   (-> (backward* nav current-doc) fix-root)
            removed-path (map
                           first
                            (drop-while
                              (fn [[a b]] (= a b))
                              (map vector path (concat (:path new-nav)
                                                       (repeat nil)))))
            ;; Old parameters to remove are the parameters from old page
            removed-params (flatten
                             (mapv
                               #(get-in nav [:structure (first %) (second %) :params])
                               removed-path))]
      (println "Removed path:" removed-path)
      (println "Params to remove:" removed-params)
      (apply update-in new-nav [:params] dissoc removed-params)))))


(defn go-to!
  "Go to a child page, relative to the root of the open document"
  [navigation new-path params]
  (swap!
    (:nav-state navigation)  
    (fn [{:keys [path] :as nav}]
      (let [;; Find out which document is currently open
            [current-doc _] (peek path)
            ;; Close current document
            previous-document (close* nav) 
            ;; Get the document of the now top-of-open docs stack
            [prev-doc _]    (peek (:path previous-document))
            ;; If document is not actually closed (previous is same as current)
            ;; then that means there were no other open documents and close*
            ;; inserted the root. In this case, the new path is already relative
            ;; to the root so we don't add it (use empty vec), but if this isn't
            ;; the case (previous doc not same as current) then the new path
            ;; needs to be relative to the document root, so append our path to
            ;; that instead of using an empty vector
            empty-vec       (if (= prev-doc current-doc) [] [[current-doc :*]])
            ;; Create new path within now-closed document
            new-path        (into empty-vec
                                  (map #(vector current-doc %) new-path))]
        ; And add new path in, effectively reopening it on new path
        (-> previous-document
            (update-in [:path] (comp vec concat) new-path)
            (fix-doc-root* :forward))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Document API


(defn close!
  "Close open document"
  [navigation]
  (let [path            (:path @(:nav-state navigation))
        [current-doc _] (peek path)]
    (doseq [_ (drop 1 (take-while #(= current-doc (first %)) path))]
      (backward! navigation :dont-fix-root))
    (backward! navigation))
  #_(swap!
    (:nav-state navigation)
    (comp #(fix-doc-root* % :forward) close*)))


(defn set-document!
  "Close all open doucments and open new document"
  [navigation document & [params]]
  (swap!
    (:nav-state navigation)
    (fn [nav]
      (-> nav
          (assoc :path [[document :*]])
          (fix-doc-root* :forward)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Data retrieval

(defn details
  "Get the page details by merging current page with all parent pages"
  [{:keys [docs path structure params]}]
  (assoc
    (reduce
      (fn [{:keys [view] :as s} doc-page-vec]
        (let [page-details (get-in structure doc-page-vec)]
          (assoc
            (merge  s (dissoc page-details :sync)) ; Merge all but :sync
            :view (merge view (:view page-details)); Merge content of :view
            :children (:children page-details)     ; Force overwrite :children
            :default  (:default page-details))))   ; ...and :default
      {}
      path)
    :params
    params))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

