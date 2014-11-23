(ns erinite.core)

(defmacro template
  [html-file opts & [spec]]
  (let [opts-map? (map? opts)
        sel       (if opts-map? (:sel opts) opts)
        page      (when opts-map? (:page opts))
        data      (when opts-map? (:data opts))]
    `(~`kioo.om/component
       ~html-file ~sel
       ~(merge
          `{[:control] ~`erinite.internal.template-transform/replace-control}    
          (when (and page data)
            `{[:placeholder]
              (partial
                ~`erinite.internal.template-transform/replace-placeholder
                ~page
                ~data)})
          spec))))

(defmacro default-renderer
  "Default renderer simply renders a kioo template with placeholder"
  [html-file & [selector]]
  (let [selector (if (nil? selector) [:.base] selector)]
    `(fn [page# data#]
      (template ~html-file {:sel ~selector :page page# :data data#}))))

