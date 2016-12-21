﻿namespace SDL.ECommerce.DXA.Servants
{
    using System;
    using System.Collections.Generic;

    using SDL.ECommerce.Api.Model;

    public class PathServant : IPathServant
    {
        private const string CATEGORIES = "/categories/";

        private const string GENERIC = "generic";

        private const string CATEGORY_PATH_SEPARATOR = "-";

        public IEnumerable<string> GetSearchPath(string url, ICategory category)
        {
            var searchPaths = new List<string>();

            var basePath = ECommerceContext.LocalizePath(CATEGORIES);

            var categoryPath = basePath;

            var currentCategory = category;

            while (currentCategory != null)
            {
                searchPaths.Add(categoryPath + currentCategory.Id);

                currentCategory = currentCategory.Parent;
            }

            var urlTokens = url.Split(new[] { '/' }, StringSplitOptions.RemoveEmptyEntries);

            foreach (var token in urlTokens)
            {
                categoryPath += token;

                searchPaths.Insert(0, categoryPath);

                categoryPath += CATEGORY_PATH_SEPARATOR;
            }

            searchPaths.Add(basePath + GENERIC);

            return searchPaths;
        }

        public IEnumerable<string> GetSearchPath(string productSeoId, IProduct product)
        {
            var searchPath = new List<string>();
            var basePath = ECommerceContext.LocalizePath("/products/");

            // SEO id
            //
            if (productSeoId != null)
            {
                searchPath.Add(basePath + productSeoId);
            }

            // Product ID
            //
            searchPath.Add(basePath + product.Id);

            // Product Categories
            //
            if (product.Categories != null)
            {
                foreach (var category in product.Categories)
                {
                    searchPath.Add(basePath + category.Id);
                }
            }

            // Generic fallback product look&feel
            //
            searchPath.Add(basePath + "generic");

            return searchPath;
        }
    }
}