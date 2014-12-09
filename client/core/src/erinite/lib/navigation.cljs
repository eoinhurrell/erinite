(ns erinite.lib.navigation)

(comment
  root-page-info  {:children "set of child pages"
                   :default "default page if root invisible, omitted if not"
                   :view    "map of placeholder ids to view ids"}
  page-info       {:children "set of child pages"
                   :view    "map of placeholder ids to view ids"}
  document-structure {document-name {:*         root-page-info
                                     page-name  page-info}}

  {:params "map of parameter ids to parameter values"
   :docs "stack (vector) of open documents"
   :path "stack (vector) of open pages"  ; Stored as vec of [doc page] pairs
   :structure document-structure }
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers


(defn make-nav-state
  "Create navigation structure"
  [document-structure initial-document]
  {:params    {}
   :path      [[initial-document :*]]
   :structure document-structure})


(defn close*
  "Close current document by stripping it from path"
  [{:keys [path] :as nav}]
  (let [[current-doc _] (peek path)
        reverse-path    (rseq path) ; Reverse so current doc can be removed
        prev-doc-path   (into [] (drop-while ; Remove current document
                                   #(= current-doc (first %))
                                   reverse-path))
        new-path        (vec (rseq prev-doc-path))]
    ;; Replace path with new path, but make sure it cannot be empty
    (assoc nav :path (if (empty? new-path) [[current-doc :*]] new-path))))


(defn forward*
  "Navigate page forward by updating path"
  [{:keys [path structure] :as nav} page-keyword]
  (let [[current-doc _] (peek path)
        doc             (namespace page-keyword)
        ;; If the page was namespaced, get the page portion
        new-page        (if doc (keyword (name page-keyword)) page-keyword)
        ;; If namespaced, get the namespace, otherwise use current doc
        new-doc         (if doc (keyword doc) current-doc)
        ;; Get children of current page
        children        (->> path peek (get-in structure) :children)
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
  "If the page is the doc root, then the page may need to be modified
    If direction is :forward, then if the root has a :default set, change the
    page to the :default instead.
    If direction is :backward, then move backward one more time"
  [{:keys [path structure] :as nav} direction]
  (let [[doc page] (peek path)]
    (if (= page :*)
      (case direction
        :forward  (if-let [default (get-in structure [doc page :default])]
                    (update-in nav [:path] conj [doc default])
                    nav)
        :backward (backward* nav doc))
      nav)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Page API


(defn forward!
  "Go to a child page (may open new document)"
  [navigation page-keyword params]
  (swap!
    (:nav-state navigation)
    (fn [nav]
      ; Navigate page forward and merge in new parameters
      (-> (forward* nav page-keyword)
          (update-in [:params] merge params)
          (fix-doc-root* :forward)))))


(defn backward!
  "Go to the parent page (may close an open document)"
  [navigation]
  (swap!
    (:nav-state navigation)
    (fn [{:keys [path] :as nav}]
      (let [[current-doc page] (peek path)]
        (apply
          update-in
          (-> (backward* nav current-doc)
              (fix-doc-root* :backward))
          [:params] ; Remove old parameters
          dissoc
          ;; Old parameters to remove are the parameters from old page
          (get-in nav [:structure current-doc page :params]))))))


(defn go-to!
  "Go to a child page, relative to the root of the open document"
  [navigation new-path params]
  (swap!
    (:nav-state navigation)  
    (fn [{:keys [path] :as nav}]
      (let [; Close current document
            previous-document (close* nav) 
            ; Create new path within now-closed document
            [current-doc _] (peek path)
            new-path        (into [[current-doc :*]]
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
  (swap!
    (:nav-state navigation)
    (comp #(fix-doc-root* % :forward) close*)))


(defn set-document!
  "Close all open doucments and open new document"
  [navigation document]
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
  [navigation]
  (let [{:keys [docs path structure params]} @(:nav-state navigation)]
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
      params)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;







(defn push-page!
  "Move to a child page by changing the path to point at the new page"
  [navigation page params]
  (swap!
    (:nav-state navigation)
    (fn [{:keys [state path] :as nav}]
      (update-in
        (if (contains? (->> path peek (get state) :children) page)
          (update-in nav [:path] conj page)
          nav)
        [:params]
        merge
        params))))


(defn pop-page!
  "Move to the parent page by changing the path to point to the new page"
  [navigation]
  (swap!
    (:nav-state navigation)
    (fn [nav]
      (apply
        update-in
        (let [new-nav (update-in nav [:path] pop)]
          (if (= (:path nav) [])
            (assoc nav :path [(:root nav)])
            new-nav))
        [:params]
        dissoc
        (get-in nav [:state (peek (:path nav)) :params])))))


(defn set-page!
  "Move to a page named by path by changing the path to point to the new page"
  [navigation page-name params]
  (swap!
    (:nav-state navigation)
    (fn [{:keys [state root]}]
      (loop [path (list page-name)]
        (if-let [parent (->> path first (get state) :parent)]
          (recur (conj path parent))
          {:path (into [] path)
           :root root
           :params params
           :state state})))))


(defn page
  "Return the page state for a page given by the current path
   NOTE: This function takes `nav-state` as its argument, not the navigation
   component like the other functions do."
  [{:keys [state path params]}]
  (assoc
    (reduce
      (fn [{:keys [view] :as s} page]
        (let [page-state (get state page)]
          (assoc
            (merge s (dissoc page-state :sync))  ; merge all state except ':sync'
            :view (merge view (:view page-state)); merge content of view
            :children (:children page-state))))  ; overwrite children
      {}
      path)
    :params
    params))


(defn set-parents
  "Add parent information to pages, given children for each page.
   NOTE: pages must not be child of more than one page - only one parent per
         page supported."
  [pages]
  (reduce
    (fn [pages [k {:keys [children]}]]
      (if children
        (reduce
          (fn [pages child]
            (assoc-in pages [child :parent] k))
          pages
          children)
        pages))
    pages
    pages))

