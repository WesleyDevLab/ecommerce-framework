﻿using Microsoft.VisualStudio.TestTools.UnitTesting;
using SDL.ECommerce.Ecl;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Text;
using System.Threading.Tasks;

namespace SDL.DemandWare.Ecl
{
    [TestClass]
    public class UnitTests
    {
        // TODO: Externalize the environment settings. Can not be included in the released version
        //

        private ProductCatalog CreateProductCatalog()
        {
            return new DemandWareProductCatalog("http://fredhopper01-tech-prtnr-eu01-dw.demandware.net/s/SiteGenesis", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        }

        [TestMethod]
        public void TestGetCategories()
        {
            var productCatalog = CreateProductCatalog();
            Category rootCategory = productCatalog.GetAllCategories();
            printCategories(rootCategory.Categories);
        }

        private void printCategories(IList<Category> categories, string prefix = "-")
        {
            foreach (Category category in categories)
            {
                Console.WriteLine(prefix + " " + category.Title + " (" + category.CategoryId + ")");
                printCategories(category.Categories, prefix + "-");
            }
        }

        [TestMethod]
        public void TestBuildCategoryPath()
        {
            var productCatalog = CreateProductCatalog();
            Category rootCategory = productCatalog.GetAllCategories();
            Category aCategory = rootCategory.Categories[2].Categories[0];

            String categoryPath = "";
            Category currentCategory = aCategory;
            while (currentCategory != null)
            {
                if (categoryPath.Length > 0)
                {
                    categoryPath = "->" + categoryPath;
                }
                categoryPath = currentCategory.Title + categoryPath;
                currentCategory = currentCategory.Parent;
            }
            Console.WriteLine("Category Path: " + categoryPath);
        }

        [TestMethod]
        public void TestGetProduct()
        {
            var productCatalog = CreateProductCatalog();
            Product product = productCatalog.GetProduct("samsung-hl67a510");
            Console.WriteLine("Product Name: " + product.Name);
            Console.WriteLine("Product Thumbnail URL: " + product.Thumbnail.Url);
            Console.WriteLine("Product Thumbnail MIME: " + product.Thumbnail.Mime);
        }

        [TestMethod]
        public void TestGetProducts()
        {
            var productCatalog = CreateProductCatalog();
            var products = productCatalog.GetProducts("electronics-televisions");
            foreach ( var product in products )
            {
                Console.WriteLine("Product: " + product.Id + ", " + product.Name);
            }
        }

        [TestMethod]
        public void TestReadProductImage()
        {
            var productCatalog = CreateProductCatalog();
            Product product = productCatalog.GetProduct("samsung-hl67a510");
            using (WebClient webClient = new WebClient())
            {
                using (Stream stream = new MemoryStream(webClient.DownloadData(product.Thumbnail.Url)))
                {
                    Console.WriteLine("Content Length:" + stream.Length + ", Type: " + product.Thumbnail.Mime);
                }
            }
        }
    }
}
