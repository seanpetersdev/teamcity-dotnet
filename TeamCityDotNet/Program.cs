using Amazon.SimpleSystemsManagement;
using Amazon.SimpleSystemsManagement.Model;
using System;


namespace TeamCityDotNet
{
    class Program
    {
        static void Main(string[] args)
        {
            string connectionString = null;
            Console.WriteLine("Hello World!");

            var request = new GetParameterRequest()
            {
                Name = "seansparamtest"
            };
            using (var client = new AmazonSimpleSystemsManagementClient(Amazon.RegionEndpoint.GetBySystemName("ap-southeast-2")))
            {
                try
                {
                    var response = client.GetParameterAsync(request).GetAwaiter().GetResult(); connectionString = response.Parameter.Value;
                    connectionString = response.Parameter.Value;
                }
                catch (Exception e)
                {
                    Console.WriteLine(e);
                }
            }

            Console.WriteLine("String is: " + connectionString);
        }
    }

}
