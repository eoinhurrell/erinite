(ns erinite.lib.navigation)

(defn push-page!
  "Move to a child page by changing the path to point at the new page"
  [navigation page]
  (swap!
    (:nav-state navigation)
    (fn [{:keys [state path] :as nav}]
      (if (contains? (->> path peek (get state) :children) page)
        (update-in nav [:path] conj page)
        nav))))


(defn pop-page!
  "Move to the parent page by changing the path to point to the new page"
  [navigation]
  (swap!
    (:nav-state navigation)
    (fn [nav]
      (if-let [new-nav (update-in nav [:path] pop)]
        new-nav
        (assoc nav :path [(:start nav)])))))


(defn set-page!
  "Move to a page named by path by changing the path to point to the new page"
  [navigation page-name]
  (swap!
    (:nav-state navigation)
    (fn [{:keys [state start]}]
      (loop [path (list page-name)]
        (if-let [parent (->> path first (get state) :parent)]
          (recur (conj path parent))
          {:path (into [] path)
           :start start
           :state state})))))


(defn page
  "Return the page state for a page given by the current path"
  [{:keys [nav-state]}]
  (let [{:keys [state path]} @nav-state]
    (reduce
      (fn [{:keys [view] :as s} page]
        (let [page-state (get state page)]
          (assoc
            (merge s (dissoc page-state :sync))  ; merge all state except ':sync'
            :view (merge view (:view page-state)); merge content of view
            :children (:children page-state))))  ; overwrite children
      {}
      path)))


(defn pages
  "Return a list of keywords naming all child pages of the current page"
  [navigation]
  (:children (page navigation)))


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

