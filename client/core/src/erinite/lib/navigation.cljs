(ns erinite.lib.navigation)

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
            (assoc nav :path [(:start nav)])
            new-nav))
        [:params]
        dissoc
        (get-in nav [:state (peek (:path nav)) :params])))))


(defn set-page!
  "Move to a page named by path by changing the path to point to the new page"
  [navigation page-name params]
  (swap!
    (:nav-state navigation)
    (fn [{:keys [state start]}]
      (loop [path (list page-name)]
        (if-let [parent (->> path first (get state) :parent)]
          (recur (conj path parent))
          {:path (into [] path)
           :start start
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

